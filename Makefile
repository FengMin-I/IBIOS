# IBIOS 发布打包
#
#   make release   跑全量测试 → 组装 dist/ibi-server-<version>/ → 打 tar.gz
#   make clean     清理 dist/
#   make help      查看用法

SHELL := /bin/bash
.DEFAULT_GOAL := help

MAVEN ?= mvn

# 版本号取根 pom 里 ibios 这个 artifact 的 <version>
VERSION := $(shell awk -F'[<>]' '/<artifactId>ibios<\/artifactId>/{f=1;next} f&&/<version>/{print $$3;exit}' pom.xml)
NAME    := ibi-server-$(VERSION)
DIST    := dist
STAGE   := $(DIST)/$(NAME)
JAR     := ibios-boot/target/ibios-boot-$(VERSION).jar
CFG_TPL := config/application.yml.example

.PHONY: help release clean

help:
	@echo "用法："
	@echo "  make release   跑全量测试并生成 $(DIST)/$(NAME).tar.gz"
	@echo "  make clean     删除 $(DIST)/"
	@echo ""
	@echo "当前版本：$(VERSION)"

release:
	@echo "==> 版本 $(VERSION)"
	$(MAVEN) clean verify
	@test -f "$(JAR)" || { echo "❌ 未找到 $(JAR)"; exit 1; }
	@test -f "$(CFG_TPL)" || { echo "❌ 未找到 $(CFG_TPL)"; exit 1; }
	@rm -rf "$(STAGE)"
	@mkdir -p "$(STAGE)/bin" "$(STAGE)/config" "$(STAGE)/libs"
	@install -m 755 bin/ibi-server "$(STAGE)/bin/ibi-server"
	@install -m 644 "$(CFG_TPL)" "$(STAGE)/config/application.yml"
	@install -m 644 "$(JAR)" "$(STAGE)/libs/"
	@cd "$(DIST)" && COPYFILE_DISABLE=1 tar -czf "$(NAME).tar.gz" "$(NAME)"
	@echo ""
	@echo "✅ $(DIST)/$(NAME).tar.gz"
	@tar -tzf "$(DIST)/$(NAME).tar.gz"

clean:
	@rm -rf "$(DIST)"
	@echo "已清理 $(DIST)/"
