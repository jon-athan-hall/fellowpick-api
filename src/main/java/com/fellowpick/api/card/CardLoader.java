package com.fellowpick.api.card;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fellowpick.api.deck.Deck;
import com.fellowpick.api.deck.DeckRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * ApplicationRunner will execute only once when the application starts up.
 */
@Component
public class CardLoader implements ApplicationRunner {
    private final CardRepository cardRepository;

    private final DeckRepository deckRepository;

    public CardLoader(CardRepository cardRepository, DeckRepository deckRepository) {
        this.cardRepository = cardRepository;
        this.deckRepository = deckRepository;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        String[] filenames = {
                "riders-of-rohan.json",
                "food-and-fellowship.json",
                "elven-council.json",
                "the-hosts-of-mordor.json"
        };

        // Loop through the decks.
        for (int i = 0; i < filenames.length; i++) {
            // Get the card data list from the file.
            List<Map<String, Object>> mainBoardData = loadMainBoardDataFromJsonFile("src/main/resources/static/" + filenames[i]);

            // Load in the correct deck, skip this whole file if there's trouble finding the deck.
            Optional<Deck> deck = deckRepository.findById((long) i + 1);
            if (deck.isEmpty()) {
                System.out.println("Deck not found with id: " + i + 1);
                continue;
            }

            // Loop through each card and create an entry in the repository.
            for (Map<String, Object> cardData : mainBoardData) {
                // Don't add the basic lands to the database.
                String originalType = (String) cardData.get("originalType");
                if (originalType != null && originalType.contains("Basic Land")) {
                    continue;
                }

                Card card = new Card();

                // JSON arrays are represented by Lists, so the colorIdentity needs to be converted.
                List<String> colorIdentityList = (List<String>) cardData.get("colorIdentity");
                Set<Color> colorIdentitySet = new HashSet<>();
                for (String color : colorIdentityList) {
                    colorIdentitySet.add(Color.valueOf(color));
                }

                // Set the rest of the card attributes and save.
                card.setId(Card.createIdFromSetCodeAndNumber(
                        (String) cardData.get("setCode"),
                        (String) cardData.get("number")
                ));
                card.setName((String) cardData.get("name"));
                card.setColorIdentity(colorIdentitySet);
                card.setDeck(deck.get());
                cardRepository.save(card);
            }
        }
    }

    private List<Map<String, Object>> loadMainBoardDataFromJsonFile(String filename) {
        try {
            // Create a file object for the JSON file.
            File file = new File(filename);

            // Instantiate an ObjectMapper for deserialization of JSON.
            ObjectMapper objectMapper = new ObjectMapper();

            // Use TypeReference for a generic Map since the JSON shape is unknown.
            Map<String, Object> jsonData = objectMapper.readValue(file, new TypeReference<Map<String, Object>>() {});

            // Extract the next layer of JSON.
            Map<String, Object> data = (Map<String, Object>) jsonData.get("data");

            // Finally, get the mainBoard list of maps, which contains data on each card.
            List<Map<String, Object>> mainBoardData = (List<Map<String, Object>>) data.get("mainBoard");
            return mainBoardData;
        } catch(Exception e) {
            System.err.println("Error loading data from JSON file: " + e.getMessage());
            return null;
        }
    }
}
