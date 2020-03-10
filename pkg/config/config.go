package config

import (
	"os"

	"github.com/citrusframework/yaks/version"
)

func GetTestBaseImage() string {
	customEnv := os.Getenv("TEST_BASE_IMAGE")
	if customEnv != "" {
		return customEnv
	}
	return getDefaultTestBaseImage()
}

func getDefaultTestBaseImage() string {
	return "yaks/yaks:" + version.Version
}
