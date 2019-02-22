package dandere2x;

import dandere2x.Utilities.DandereUtils;
import dandere2x.Utilities.VectorDisplacement;
import wrappers.Frame;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import static java.io.File.separator;
import static java.lang.System.exit;

public class Merge implements Runnable {

    public int blockSize;
    public int bleed;
    public String workspace;
    public int frameCount;
    public int lexiConstant = 6;
    public PrintStream log;

    public Merge(int blockSize, int bleed, String workspace, int frameCount) {
        this.blockSize = blockSize;
        this.bleed = bleed;
        this.workspace = workspace;
        this.frameCount = frameCount;

        try {
            log = new PrintStream(new File(workspace + "logs" + separator + "merge_logfile.txt"));
        } catch (FileNotFoundException e) {
            System.out.println("Fatal Error: Could not create file at " + workspace + "logs" + separator + "merge_logfile.txt");
        }
    }


    /*
    There's a 'base' image in which all merged photos are derived from, even if all the pixels
    are eventually overwritten. This is similiar to the genisis block in bitcoin.

    Using the information from inversion and pdifferences, reconstruct the 4k image.
     */

    public void run() {
        Frame base = DandereUtils.listenImage(log, workspace + "merged" + separator + "merged_" + 1 + ".jpg");

        for (int x = 1; x < frameCount; x++) {
            log.println("Mering frame " + x);
            String inputName;

            if (DandereUtils.isLinux())
                inputName = workspace + "upscaled" + separator + "output_" + x + ".png";
            else
                inputName = workspace + "upscaled" + separator + "output_" + DandereUtils.getLexiconValue(lexiConstant, x) + ".png";

            Frame inversion = DandereUtils.listenImage(log, inputName);
            List<String> listPredictive = DandereUtils.listenText(log, workspace + "pframe_data" + separator + "pframe_" + x + ".txt");
            List<String> listInversion = DandereUtils.listenText(log, workspace + "inversion_data" + separator + "inversion_" + x + ".txt");

            base = createPredictive(x, inversion, base,
                    listPredictive, listInversion,
                    workspace + "merged" + separator + "merged_" + (x + 1) + ".jpg");

        }
    }


    /**
     *
     * @param frame framenumber is essentially unused, just here for debuggign
     * @param inversion image containing the missing parts
     * @param base the previous frame in which we draw over
     * @param listPredictive vectors of predictive /interpolated frames
     * @param listInversion vectors needed to piece inversion back into the larger image
     * @param outLocation
     * @returns a image craeted from the base, modifies the pixels based on predictive, and draws the inversion frames over the respective location.
     */
    private Frame createPredictive(int frame, Frame inversion, Frame base, List<String> listPredictive, List<String> listInversion, String outLocation) {
        Frame out = new Frame(base.width, base.height);

        ArrayList<VectorDisplacement> vectorDisplacements = new ArrayList<>();
        ArrayList<VectorDisplacement> inversionDisplacements = new ArrayList<>();


        //read every predictive vector and put it into an arraylist
        for (int x = 0; x < listPredictive.size() / 4; x++) {
            vectorDisplacements.add(
                    new VectorDisplacement(Integer.parseInt(listPredictive.get(x * 4)), Integer.parseInt(listPredictive.get(x * 4 + 1)),
                            Integer.parseInt(listPredictive.get(x * 4 + 2)),
                            Integer.parseInt(listPredictive.get(x * 4 + 3))));
        }

        //read every inversion vector and put it into an arraylist
        for (int x = 0; x < listInversion.size() / 4; x++) {
            inversionDisplacements.add(
                    new VectorDisplacement(Integer.parseInt(listInversion.get(x * 4)), Integer.parseInt(listInversion.get(x * 4 + 1)),
                            Integer.parseInt(listInversion.get(x * 4 + 2)),
                            Integer.parseInt(listInversion.get(x * 4 + 3))));
        }


        //if it is the case that both lists are empty, then the upscaled image is the new frame.
        if (inversionDisplacements.isEmpty() && vectorDisplacements.isEmpty()) {
            log.println("frame is a brand new frame, saving frame");
            out = inversion;
            out.saveFile(outLocation);
            return out;
        }

        //if it is a pFrame but we don't have any inversion items, then simply copy the previous frame.
        if (inversionDisplacements.isEmpty() && !vectorDisplacements.isEmpty()) {
            log.println("frame is identical to previous frame");
            base.saveFile(outLocation);
            return base;
        }

        try {
            //piece together the image using predictive information
            for (int outer = 0; outer < vectorDisplacements.size(); outer++) {
                for (int x = 0; x < blockSize * 2; x++) {
                    for (int y = 0; y < blockSize * 2; y++) {
                        out.set(x + 2 * vectorDisplacements.get(outer).x, y + 2 * vectorDisplacements.get(outer).y,
                                base.getNoThrow(x + 2 * vectorDisplacements.get(outer).newX, y + 2 * vectorDisplacements.get(outer).newY));
                    }
                }
            }

            //put inversion (the missing) information into the image
            for (int outer = 0; outer < inversionDisplacements.size(); outer++) {
                for (int x = 0; x < (blockSize * 2); x++) {
                    for (int y = 0; y < (blockSize * 2); y++) {
                        out.set(inversionDisplacements.get(outer).x * 2 + x, inversionDisplacements.get(outer).y * 2 + y,
                                inversion.get(inversionDisplacements.get(outer).newX * (2 * (blockSize + bleed)) + x + bleed, inversionDisplacements.get(outer).newY * (2 * (blockSize + bleed)) + y + bleed));

                    }
                }
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            log.println("Critical Error: Frame " + frame + " caused an unexpected out of bounds exception");
            log.println(e.toString());
            log.println("Program will terminate");
            exit(1);
        }

        log.println("Saving frame " + frame);
        //save the new predictive frame
        out.saveFile(outLocation);

        //reduce time needed at runtime by returning the new image as to not have to load it again
        return out;
    }


}