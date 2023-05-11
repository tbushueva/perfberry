package main

import (
	"log"
	"os"

	"github.com/tbushueva/perfberry/perfberry-cli/api"
	"github.com/tbushueva/perfberry/perfberry-cli/cmd"
	"github.com/tbushueva/perfberry/perfberry-cli/helpers"
	"github.com/tbushueva/perfberry/perfberry-cli/ui"
	"github.com/urfave/cli"
)

const (
	appName = "Perfberry CLI"
	version = "1.0.0"
)

type App struct {
	CLI *cli.App
	API *api.Client
	UI  *ui.Client
}

func (a *App) Run() {
	helpers.ClearStatus()

	if err := a.CLI.Run(os.Args); err != nil {
		log.Fatalln(err)
	}

	if code, err := helpers.ReadStatus(); err == nil {
		os.Exit(code)
	}
}

var app App

func init() {
	app.API = api.NewClient().SetUserAgent(appName + "/" + version)
	app.UI = ui.NewClient(app.API)
	app.CLI = cmd.NewCmd(appName, version, app.API, app.UI)
}

func main() {
	app.Run()
}
