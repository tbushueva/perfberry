package ui

import (
	"strconv"

	"github.com/tbushueva/perfberry/perfberry-cli/api"
	"github.com/tbushueva/perfberry/perfberry-cli/models"
)

const defaultBaseURL = "http://localhost:3000"

type Client struct {
	baseURL   string
	ApiClient *api.Client
}

func NewClient(c *api.Client) *Client {
	return &Client{
		baseURL:   defaultBaseURL,
		ApiClient: c,
	}
}

func (c *Client) SetURL(new string) {
	c.baseURL = new
}

func (c *Client) ReportLink(projectId int, r *models.Report) string {
	project, _ := c.ApiClient.Project(projectId) // TODO catch error

	return c.baseURL + "/projects/" + project.Alias + "/reports/" + strconv.Itoa(r.ID)
}
