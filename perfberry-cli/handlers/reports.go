package handlers

import (
	"io/ioutil"
	"log"
	"strconv"

	"github.com/tbushueva/perfberry/perfberry-cli/api"
	"github.com/tbushueva/perfberry/perfberry-cli/models"
	"github.com/tbushueva/perfberry/perfberry-cli/ui"
)

func CreateReport(
	projectID int,
	inputFile string,
	outputIdFile string,
	outputFile string,
	ac *api.Client,
	uc *ui.Client,
) (err error) {
	//TODO optional input
	log.Println("Reading report from", inputFile, "...")
	report, err := models.NewReportFromFile(inputFile)
	if err != nil {
		return
	}

	log.Println("Posting provided report ...")
	createdReport, err := ac.PostReport(projectID, report)
	if err != nil {
		return
	}

	log.Println("Report link:", uc.ReportLink(projectID, createdReport))

	if outputIdFile != "" {
		log.Println("Saving created report id to", outputIdFile, "...")
		err = ioutil.WriteFile(outputIdFile, []byte(strconv.Itoa(createdReport.ID)), 0644)
	}

	if outputFile != "" {
		log.Println("Saving created report to", outputFile, "...")
		err = createdReport.ToFile(outputFile)
	}

	return
}
