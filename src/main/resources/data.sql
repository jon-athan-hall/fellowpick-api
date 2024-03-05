DROP TABLE card_color_identity;
DELETE FROM card;
DELETE FROM deck;

INSERT INTO deck (id, name) VALUES (1, 'Riders of Rohan');
INSERT INTO deck (id, name) VALUES (2, 'Food and Fellowship');
INSERT INTO deck (id, name) VALUES (3, 'Elven Council');
INSERT INTO deck (id, name) VALUES (4, 'The Hosts of Mordor');
