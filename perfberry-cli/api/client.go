package api

import (
	"bytes"
	"io"
	"io/ioutil"
	"mime/multipart"
	"net"
	"net/http"
	"net/url"
	"os"
	"strconv"
	"time"

	"github.com/tbushueva/perfberry/perfberry-cli/models"
)

const defaultBaseURL = "http://api:9000"
const defaultUserAgent = "Perfberry CLI"

type Client struct {
	httpClient *http.Client
	baseURL    string
	userAgent  string
}

func (c *Client) doRequest(
	method string,
	path string,
	body io.Reader,
	headers map[string]string,
) (*http.Response, error) {
	request, err := http.NewRequest(method, c.baseURL+path, body)
	if err != nil {
		return nil, err
	}

	request.Header.Set("User-Agent", c.userAgent)
	for header, value := range headers {
		request.Header.Add(header, value)
	}

	return c.httpClient.Do(request)
}

func (c *Client) readBody(r *http.Response) ([]byte, error) {
	bb, err := ioutil.ReadAll(r.Body)

	defer func() {
		if derr := r.Body.Close(); derr != nil && err == nil {
			err = derr
		}
	}()

	return bb, err
}

func (c *Client) SetURL(new string) *Client {
	c.baseURL = new
	return c
}

func (c *Client) SetUserAgent(new string) *Client {
	c.userAgent = new
	return c
}

func NewClient() *Client {
	transport := &http.Transport{
		DialContext: (&net.Dialer{
			Timeout:   30 * time.Second,
			KeepAlive: 30 * time.Second,
		}).DialContext,
		TLSHandshakeTimeout: 5 * time.Second,
	}

	client := &http.Client{
		Timeout:   time.Second * 60,
		Transport: transport,
	}

	return &Client{
		httpClient: client,
		baseURL:    defaultBaseURL,
		userAgent:  defaultUserAgent,
	}
}

func (c *Client) PostReport(projectID int, r *models.Report) (*models.Report, error) {
	b, err := r.ToJSON()
	if err != nil {
		return nil, err
	}

	reqUrl := "/v1/projects/" + strconv.Itoa(projectID) + "/reports"
	headers := map[string]string{"Content-Type": "application/json"}
	response, err := c.doRequest(http.MethodPost, reqUrl, bytes.NewReader(b), headers)
	if err != nil {
		return nil, err
	}

	bb, err := c.readBody(response)
	if err != nil {
		return nil, err
	}

	return models.NewReportFromJSON(bb)
}

func (c *Client) PostLog(
	projectID int,
	format string,
	extended bool,
	reportId int,
	files []string,
	r *models.Report,
	b *models.Build,
	a *models.Assertions,
) (*models.Report, error) {
	body := &bytes.Buffer{}
	writer := multipart.NewWriter(body)

	for _, path := range files {
		part, err := writer.CreateFormFile("data", path)
		if err != nil {
			return nil, err
		}
		file, err := os.Open(path)
		if err != nil {
			return nil, err
		}
		_, err = io.Copy(part, file)
		if err != nil {
			return nil, err
		}
		file.Close()
	}

	if r != nil {
		reportBody, err := r.ToJSON()
		if err != nil {
			return nil, err
		}

		err = writer.WriteField("report", string(reportBody))
		if err != nil {
			return nil, err
		}
	}

	if b != nil {
		buildBody, err := b.ToJSON()
		if err != nil {
			return nil, err
		}

		err = writer.WriteField("build", string(buildBody))
		if err != nil {
			return nil, err
		}
	}

	if a != nil {
		assertionsBody, err := a.ToJSON()
		if err != nil {
			return nil, err
		}

		err = writer.WriteField("assertions", string(assertionsBody))
		if err != nil {
			return nil, err
		}
	}

	err := writer.Close()
	if err != nil {
		return nil, err
	}

	reqUrl, _ := url.Parse("/v2/projects/" + strconv.Itoa(projectID) + "/logs/" + format)
	query := url.Values{}
	if extended {
		query.Add("extended", "true")
	}
	if reportId > 0 {
		query.Add("report_id", strconv.Itoa(reportId))
	}
	reqUrl.RawQuery = query.Encode()
	headers := map[string]string{"Content-Type": writer.FormDataContentType()}
	response, err := c.doRequest(http.MethodPost, reqUrl.String(), body, headers)
	if err != nil {
		return nil, err
	}

	bb, err := c.readBody(response)
	if err != nil {
		return nil, err
	}

	return models.NewReportFromJSON(bb)
}

func (c *Client) Project(id int) (*models.Project, error) {
	reqUrl := "/v1/projects/" + strconv.Itoa(id)

	response, err := c.doRequest(http.MethodGet, reqUrl, nil, nil)
	if err != nil {
		return nil, err
	}

	bb, err := c.readBody(response)
	if err != nil {
		return nil, err
	}

	return models.NewProjectFromJSON(bb)
}
