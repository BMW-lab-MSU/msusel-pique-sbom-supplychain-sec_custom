/**
 * MIT License
 * Copyright (c) 2019 Montana State University Software Engineering Labs
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package runnable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pique.analysis.ITool;
import pique.evaluation.Project;
import pique.runnable.ASingleProjectEvaluator;
import pique.utility.PiqueProperties;
import tool.GrypeWrapper;
import tool.TrivySBOMGenerationWrapper;
import tool.TrivyWrapper;
import tool.sbomqsWrapper;

/**
 * Behavioral class responsible for running TQI evaluation of a single project
 */
// TODO (1.0): turn into static methods (maybe unless logger problems)
public class SingleProjectEvaluator extends ASingleProjectEvaluator {
    private static final Logger LOGGER = LoggerFactory.getLogger(SingleProjectEvaluator.class);

    //default properties location
    @Getter @Setter
    private String propertiesLocation = "src/main/resources/pique-properties.properties";

    public SingleProjectEvaluator(String sbomDirectory, String sourceCodeDirectory) {
        init(sbomDirectory, sourceCodeDirectory);
    }

    public void init(String sbomDirectory, String sourceCodeDirectory){
        LOGGER.info("Starting Analysis");
        Properties prop = null;
        try {
            prop = propertiesLocation == null ? PiqueProperties.getProperties() : PiqueProperties.getProperties(propertiesLocation);
        } catch (IOException e) {
            e.printStackTrace();
        }


        Path sbomPath = Paths.get(sbomDirectory);
        Path sourceCodePath = Paths.get(sourceCodeDirectory);
        Path resultsDir = Paths.get(prop.getProperty("results.directory"));

        /**
         * Code that checks if source code is present to generate SBOMs for, we iterate through each directory or
         * file in the source code directory. We generate SBOMs (currently using Trivy) for each project in the
         * source code directory. The resulting SBOMs are placed in the input/projects/SBOM directory. The code
         * then proceeds to evaluate each SBOM in the input/projects/SBOM directory.
         */
//        Set<Path> sourceCodeRoots = new HashSet<>();
//        File[] sourceCodeToGenerateSbomsFrom = sourceCodePath.toFile().listFiles();
//        assert sourceCodeToGenerateSbomsFrom != null; // Ensure the directory is not empty
//        for (File f : sourceCodeToGenerateSbomsFrom) {
//            if (!f.getName().equals(".gitignore")) { // Check to avoid adding .gitignore
//                sourceCodeRoots.add(f.toPath()); // Add both files and directories except .gitignore
//            }
//        }

//        TrivySBOMGenerationWrapper trivySBOMGenerator = new TrivySBOMGenerationWrapper();

//        // Generate SBOMs for each project in the source code directory
//        for (Path projectToGenerateSbomFor : sourceCodeRoots){
//            LOGGER.info("Generating SBOM for: {}", projectToGenerateSbomFor.toString());
//            System.out.println("Generating SBOM for: " + projectToGenerateSbomFor);
//            trivySBOMGenerator.generate(projectToGenerateSbomFor);
//        }

        // get derived quality model location
        Path qmLocation = Paths.get(prop.getProperty("derived.qm"));

        // initialize SBOM analysis tools that will run on each SBOM in the input/projects/SBOM directory
        ITool gyrpeWrapper = new GrypeWrapper(prop.getProperty("github-token-path"));
        ITool trivyWrapper = new TrivyWrapper(prop.getProperty("github-token-path"));
//        ITool sbomqsWrapper_ = new sbomqsWrapper();
        Set<ITool> tools = Stream.of(gyrpeWrapper,trivyWrapper).collect(Collectors.toSet());

        // loop through each SBOM in the input/projects/SBOM directory and store paths in a list
        Set<Path> sbomRoots = new HashSet<>();
        File[] sbomsToAssess = sbomPath.toFile().listFiles();
        assert sbomsToAssess != null;
        for (File f : sbomsToAssess){
            if (f.isFile() && !f.getName().equals(".gitignore")){
                sbomRoots.add(f.toPath());
            }
        }

        // PIQUE entry point - evaluates each SBOM in the input/projects/SBOM directory and stores results in out directory
        for (Path projectUnderAnalysisPath : sbomRoots){
            LOGGER.info("Project to analyze: {}", projectUnderAnalysisPath.toString());
            Path outputPath = runEvaluator(projectUnderAnalysisPath, resultsDir, qmLocation, tools);
            LOGGER.info("output: {}", outputPath.getFileName());
            System.out.println("output: " + outputPath.getFileName());
            System.out.println("exporting compact: " + project.exportToJson(resultsDir, true));
        }
    }
    //region Get / Set
    public Project getEvaluatedProject() {
        return project;
    }


}
