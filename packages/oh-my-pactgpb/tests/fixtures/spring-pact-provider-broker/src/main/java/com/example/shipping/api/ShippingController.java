package com.example.shipping.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/shipments")
public class ShippingController {

  @GetMapping("/{id}")
  public String getShipment(@PathVariable String id) {
    return id;
  }
}
