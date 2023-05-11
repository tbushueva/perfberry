package helpers

import (
	"bytes"
	"errors"
	"io/ioutil"
	"os"
	"text/template"
)

func resolveEnv(key string) string {
	return os.Getenv(key)
}

// TODO add catching error
func resolveFile(path string) (string, error) {
	bts, err := ioutil.ReadFile(path)
	return string(bts), err
}

func sub(from int, to int, s string) (string, error) {
	if from >= len(s) || to > len(s) {
		return s, errors.New("invalid range definition") //TODO
	}

	if to == -1 {
		to = len(s)
	}

	return s[from:to], nil
}

func ProccessTemplate(text []byte) ([]byte, error) {
	funcs := template.FuncMap{
		"env":  resolveEnv,
		"file": resolveFile,
		"sub":  sub,
	}

	templ, err := template.New("templ").Funcs(funcs).Parse(string(text))
	if err != nil {
		return nil, err
	}

	buffer := &bytes.Buffer{}
	err = templ.Execute(buffer, nil)
	return buffer.Bytes(), err
}

func ProccessTemplateFile(path string) ([]byte, error) {
	bts, err := ioutil.ReadFile(path)
	if err != nil {
		return nil, err
	}

	return ProccessTemplate(bts)
}
