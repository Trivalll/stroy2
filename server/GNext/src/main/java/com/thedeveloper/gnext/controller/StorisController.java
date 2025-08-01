package com.thedeveloper.gnext.controller;

import com.thedeveloper.gnext.entity.ChatEntity;
import com.thedeveloper.gnext.entity.StorisEntity;
import com.thedeveloper.gnext.entity.UserEntity;
import com.thedeveloper.gnext.enums.HistoryContent;
import com.thedeveloper.gnext.service.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.thedeveloper.gnext.utils.Globals;
import reactor.core.publisher.Mono;

import java.util.Date;

@RestController
@RequestMapping("api/v1/storis")
@AllArgsConstructor
@Slf4j
public class StorisController {
    ChatService chatService;
    UserService userService;
    StorisService storisService;
    StoriesService imageService;
    ResourceLoader resourceLoader;
    @PostMapping("/add")
    public ResponseEntity<?> addStoris(@RequestParam("chat") String chat_id, @RequestParam HistoryContent type, @RequestParam String uid, @RequestBody MultipartFile file){
        UserEntity user = userService.findUserByUid(uid);
        ChatEntity chat_ = chatService.findById(chat_id);
        imageService.store(file);
        String file_name = Globals.renameFileHistory(file.getOriginalFilename(), type, imageService);
        StorisEntity storis = new StorisEntity();
        storis.setChat(chat_);
        storis.setUser(user);
        storis.setType(type);
        storis.setContent(file_name);
        storis.setCreateDate(new Date());
        storisService.save(storis);
        return new ResponseEntity<>(HttpStatus.OK);
    }
    @GetMapping("/chat")
    public ResponseEntity<?> getStory(@RequestParam("uid")String uid,@RequestParam("chat") String chat_name){
        UserEntity user = userService.findUserByUid(uid);
        ChatEntity chat = chatService.findChatByLocationAndName(user.getLocation(),chat_name);
       return new ResponseEntity<>(storisService.getStorisByChat(chat), HttpStatus.OK);
    }
    @GetMapping("/photo/{name}")
    public ResponseEntity<?> getAvatar(@PathVariable String name){
        try {
            Resource image = imageService.loadAsResource(name);
            return  ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(image);
        }catch (Exception e){
            log.debug("Image Error {}", e.toString());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
    @GetMapping(value = "/video/{name}",produces = "video/mp4")
    public Mono<Resource> streamVideo(@PathVariable("name") String fileName) {
        Resource video = imageService.loadAsResource(fileName);

        return Mono.fromSupplier(() -> video);
    }
}
