package com.example.commerce.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/commerce")
public class CommerceController {

  @GetMapping("/status")
  public String status() {
    return "ok";
  }
}
