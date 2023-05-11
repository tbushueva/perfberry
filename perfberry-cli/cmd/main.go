package cmd

import (
	"net/url"

	"github.com/tbushueva/perfberry/perfberry-cli/api"
	"github.com/tbushueva/perfberry/perfberry-cli/ui"
	"github.com/urfave/cli"
)

func NewCmd(appName string, version string, ac *api.Client, uc *ui.Client) *cli.App {
	return &cli.App{
		Name:    appName,
		Usage:   "works with your performance reports in Perfberry from console",
		Version: version,
		Flags: []cli.Flag{
			&cli.StringFlag{
				Name:  "api-url",
				Usage: "override API url",
			},
			&cli.StringFlag{
				Name:  "ui-url",
				Usage: "override UI url",
			},
		},
		Before: func(c *cli.Context) error {
			if apiURL := c.String("api-url"); apiURL != "" {
				parsedURL, err := url.ParseRequestURI(apiURL)
				if err != nil {
					return err
				}
				ac.SetURL(parsedURL.String())
			}

			if uiURL := c.String("ui-url"); uiURL != "" {
				parsedURL, err := url.ParseRequestURI(uiURL)
				if err != nil {
					return err
				}
				uc.SetURL(parsedURL.String())
			}

			return nil
		},
		Commands: []cli.Command{
			logsCmd(ac, uc),
			reportsCmd(ac, uc),
			playbookCmd(ac, uc),
		},
	}
}
