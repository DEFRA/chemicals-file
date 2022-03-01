package uk.gov.defra.reach.file.controller;

import java.io.IOException;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.defra.reach.file.Container;
import uk.gov.defra.reach.file.SerializableChecksum;
import uk.gov.defra.reach.file.SerializableUri;
import uk.gov.defra.reach.file.service.FileService;

@RestController
@RequestMapping("/file")
public class FileServiceController {

  private final FileService fileService;

  public FileServiceController(FileService fileService) {
    this.fileService = fileService;
  }

  /**
   * Allows a client to retrieve a previously-persisted file from the specified storage container.
   *
   * @param container the container
   * @param target the target
   */
  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  public SerializableUri get(@RequestParam("container") Container container, @RequestParam("target") String target) {
    URI uri = fileService.get(container, target);
    return SerializableUri.from(uri);
  }

  /**
   * Allows a client to store a file to a container
   *
   * @param file the file
   * @param container the container
   * @param target destination filename on Storage Container
   */
  @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<SerializableChecksum> store(@RequestParam("file") MultipartFile file, @RequestParam("container") Container container,
      @RequestParam("target") String target)
      throws IOException {
    String checksum  = fileService.store(file.getInputStream(), container, target);
    return new ResponseEntity<>(SerializableChecksum.from(checksum), HttpStatus.CREATED);
  }

  @DeleteMapping("/{container}/{fileName}")
  void delete(@PathVariable("container") Container container, @PathVariable("fileName") String fileName) {
    fileService.delete(container, fileName);
  }

  /**
   * Checks if a file exists
   *
   * @param container the container
   * @param target the storage location of the file
   */
  @RequestMapping(method = RequestMethod.HEAD)
  void exists(@RequestParam("container") Container container, @RequestParam("target") String target) {
    fileService.checkFileExists(container, target);
  }

}
