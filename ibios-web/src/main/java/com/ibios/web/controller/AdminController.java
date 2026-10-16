package com.ibios.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminController {

  @GetMapping({"/admin", "/admin/"})
  public String adminIndex() {
    return "forward:/admin/index.html";
  }
}
