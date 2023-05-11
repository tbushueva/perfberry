package helpers

import (
	"io/ioutil"
	"os"
	"strconv"
)

const statusFilePath = "status.txt"

func ClearStatus() error {
	return os.Remove(statusFilePath)
}

func WriteStatus(s int) error {
	err := ioutil.WriteFile(statusFilePath, []byte(strconv.Itoa(s)), 0644)

	return err
}

func ReadStatus() (int, error) {
	bts, err := ioutil.ReadFile(statusFilePath)
	if err != nil {
		return 0, err
	}

	i, err := strconv.Atoi(string(bts))
	if err != nil {
		return 0, err
	}

	return i, nil
}
