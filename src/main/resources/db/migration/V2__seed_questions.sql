-- V2__seed_questions.sql
-- Sample questions for local development
-- difficulty values must match QuestionDifficulty enum:
-- TWO_HUNDRED, FOUR_HUNDRED, SIX_HUNDRED, EIGHT_HUNDRED, ONE_THOUSAND

INSERT INTO questions (category, point_value, clue, answer, difficulty) VALUES
-- Science
('Science', 200,  'This force keeps planets in orbit around the sun',           'What is gravity?',        'TWO_HUNDRED'),
('Science', 400,  'The chemical symbol for gold',                               'What is Au?',             'FOUR_HUNDRED'),
('Science', 600,  'The process by which plants convert sunlight into food',     'What is photosynthesis?', 'SIX_HUNDRED'),
('Science', 800,  'This particle has no electric charge and is found in atomic nuclei', 'What is a neutron?', 'EIGHT_HUNDRED'),
('Science', 1000, 'The SI unit of electrical resistance',                       'What is the ohm?',        'ONE_THOUSAND'),

-- History
('History', 200,  'The year World War II ended',                                'What is 1945?',                        'TWO_HUNDRED'),
('History', 400,  'The ancient wonder that stood in Alexandria, Egypt',         'What is the Lighthouse of Alexandria?','FOUR_HUNDRED'),
('History', 600,  'The U.S. president during the Cuban Missile Crisis',         'Who is John F. Kennedy?',              'SIX_HUNDRED'),
('History', 800,  'The empire ruled by Genghis Khan',                           'What is the Mongol Empire?',           'EIGHT_HUNDRED'),
('History', 1000, 'The year the Berlin Wall fell',                              'What is 1989?',                        'ONE_THOUSAND'),

-- Geography
('Geography', 200,  'The longest river in the world',                           'What is the Nile?',           'TWO_HUNDRED'),
('Geography', 400,  'The smallest country in the world by area',                'What is Vatican City?',       'FOUR_HUNDRED'),
('Geography', 600,  'The capital city of Australia',                            'What is Canberra?',           'SIX_HUNDRED'),
('Geography', 800,  'The mountain range that separates Europe from Asia',       'What are the Ural Mountains?','EIGHT_HUNDRED'),
('Geography', 1000, 'The only country that borders both the Atlantic and Indian Oceans', 'What is South Africa?', 'ONE_THOUSAND'),

-- Technology
('Technology', 200,  'The language used to structure web pages',                'What is HTML?',             'TWO_HUNDRED'),
('Technology', 400,  'This company created the Java programming language',      'What is Sun Microsystems?', 'FOUR_HUNDRED'),
('Technology', 600,  'The design pattern that separates an app into Model, View, and Controller', 'What is MVC?', 'SIX_HUNDRED'),
('Technology', 800,  'A data structure that follows Last In, First Out ordering','What is a stack?',         'EIGHT_HUNDRED'),
('Technology', 1000, 'The protocol that WebSockets upgrade from',               'What is HTTP?',             'ONE_THOUSAND'),

-- Pop Culture
('Pop Culture', 200,  'The fictional kingdom in the movie Frozen',              'What is Arendelle?',      'TWO_HUNDRED'),
('Pop Culture', 400,  'The author of the Harry Potter series',                  'Who is J.K. Rowling?',    'FOUR_HUNDRED'),
('Pop Culture', 600,  'The TV show set in the fictional land of Westeros',      'What is Game of Thrones?','SIX_HUNDRED'),
('Pop Culture', 800,  'This band released the album "Abbey Road" in 1969',     'Who are The Beatles?',    'EIGHT_HUNDRED'),
('Pop Culture', 1000, 'The name of Tony Stark''s AI assistant in Iron Man',    'Who is J.A.R.V.I.S.?',   'ONE_THOUSAND'),

-- Sports
('Sports', 200,  'The number of players on a basketball team on the court at one time', 'What is 5?',                        'TWO_HUNDRED'),
('Sports', 400,  'The country that has won the most FIFA World Cups',           'What is Brazil?',                           'FOUR_HUNDRED'),
('Sports', 600,  'The tennis tournament played on grass courts in England',     'What is Wimbledon?',                        'SIX_HUNDRED'),
('Sports', 800,  'The Olympic motto in Latin meaning "Faster, Higher, Stronger"','What is Citius, Altius, Fortius?',         'EIGHT_HUNDRED'),
('Sports', 1000, 'The term for scoring three goals in a single hockey game',   'What is a hat trick?',                      'ONE_THOUSAND');