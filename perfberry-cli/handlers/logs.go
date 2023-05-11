package handlers

import (
	"errors"
	"log"

	"github.com/tbushueva/perfberry/perfberry-cli/api"
	"github.com/tbushueva/perfberry/perfberry-cli/helpers"
	"github.com/tbushueva/perfberry/perfberry-cli/models"
	"github.com/tbushueva/perfberry/perfberry-cli/ui"
)

const browsertimeLogType = "browsertime"
const gatlingLogType = "gatling"
const phantomLogType = "phantom"

const browsertimeLogFileName = "browsertime\\.pageSummary\\.json"
const gatlingLogFileName = "simulation\\.log"
const phantomLogFileName = "phout_.*\\.log"

func UploadLog(
	projectID int,
	format string,
	extended bool,
	reportId int,
	dir string,
	reportFile string,
	buildFile string,
	assertionsFile string,
	outputFile string,
	followStatus bool,
	ac *api.Client,
	uc *ui.Client,
) error {
	var report *models.Report
	if reportFile != "" {
		log.Println("Reading report from", reportFile, "...")
		r, err := models.NewReportFromFile(reportFile)
		if err != nil {
			return err
		}
		report = r
	}

	var build *models.Build
	if buildFile != "" {
		log.Println("Reading build from", buildFile, "...")
		b, err := models.NewBuildFromFile(buildFile)
		if err != nil {
			return err
		}
		build = b
	}

	var assertions *models.Assertions
	if assertionsFile != "" {
		log.Println("Reading assertions from", assertionsFile, "...")
		a, err := models.NewAssertionsFromFile(assertionsFile)
		if err != nil {
			return err
		}
		assertions = a
	}

	var logFileName string
	switch format {
	case browsertimeLogType:
		logFileName = browsertimeLogFileName
	case gatlingLogType:
		logFileName = gatlingLogFileName
	case phantomLogType:
		logFileName = phantomLogFileName
	}
	log.Println("Searching logs at", dir, "...")
	files, err := helpers.SearchFiles(dir, logFileName)
	if err != nil {
		return errors.New("Couldn't find " + logFileName + " at " + dir)
	}

	if len(files) == 0 {
		log.Println("Not found", logFileName, "files, skips upload...")
		return nil
	}

	log.Println("Posting this logs:")
	for _, v := range files {
		log.Println(v)
	}
	createdReport, err := ac.PostLog(projectID, format, extended, reportId, files, report, build, assertions)
	if err != nil {
		return err
	}

	log.Println("Report link:", uc.ReportLink(projectID, createdReport))

	if followStatus {
		log.Println("Checks assertions and report status...")
		if createdReport.Passed != nil {
			if *createdReport.Passed {
				log.Println("Status is PASSED.")
			} else {
				helpers.WriteStatus(1)
				log.Println("Status is FAILED.")
			}
		} else {
			log.Println("Report has not status.")
		}
	}

	if outputFile != "" {
		log.Println("Saving created report to", outputFile, "...")
		err = createdReport.ToFile(outputFile)
	}

	return nil
}
