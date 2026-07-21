-- Echo Library V2: Supabase Database Schema Migration
-- Designed for PostgreSQL 15+ with high optimization for search, personalization, and AI features.

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Trigger function to update updated_at timestamps automatically
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

---------------------------------------------------------
-- 1. USERS PROFILE TABLE
---------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY, -- Match with Supabase auth.users.id
    email VARCHAR UNIQUE NOT NULL,
    username VARCHAR UNIQUE NOT NULL,
    display_name VARCHAR,
    avatar_url VARCHAR,
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT NOW() NOT NULL
);

CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users 
FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Enable RLS
ALTER TABLE users ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Public profiles are viewable by everyone" ON users
    FOR SELECT USING (true);

CREATE POLICY "Users can update their own profiles" ON users
    FOR UPDATE USING (auth.uid() = id);

---------------------------------------------------------
-- 2. SPEAKERS TABLE
---------------------------------------------------------
CREATE TABLE IF NOT EXISTS speakers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR NOT NULL,
    bio TEXT,
    avatar_url VARCHAR,
    followers_count INT DEFAULT 0 NOT NULL,
    audio_count INT DEFAULT 0 NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT NOW() NOT NULL
);

CREATE TRIGGER update_speakers_updated_at BEFORE UPDATE ON speakers 
FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Enable RLS
ALTER TABLE speakers ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Speakers are viewable by everyone" ON speakers
    FOR SELECT USING (true);

---------------------------------------------------------
-- 3. CATEGORIES TABLE
---------------------------------------------------------
CREATE TABLE IF NOT EXISTS categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR UNIQUE NOT NULL,
    count INT DEFAULT 0 NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT NOW() NOT NULL
);

CREATE TRIGGER update_categories_updated_at BEFORE UPDATE ON categories 
FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Enable RLS
ALTER TABLE categories ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Categories are viewable by everyone" ON categories
    FOR SELECT USING (true);

---------------------------------------------------------
-- 4. PROVIDERS TABLE
---------------------------------------------------------
CREATE TABLE IF NOT EXISTS providers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR NOT NULL UNIQUE,
    api_base_url VARCHAR,
    is_active BOOLEAN DEFAULT TRUE NOT NULL,
    metadata JSONB DEFAULT '{}'::jsonb NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT NOW() NOT NULL
);

CREATE TRIGGER update_providers_updated_at BEFORE UPDATE ON providers 
FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Enable RLS
ALTER TABLE providers ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Providers are viewable by everyone" ON providers
    FOR SELECT USING (true);

---------------------------------------------------------
-- 5. PODCASTS TABLE
---------------------------------------------------------
CREATE TABLE IF NOT EXISTS podcasts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR NOT NULL,
    host VARCHAR NOT NULL,
    episodes_count INT DEFAULT 0 NOT NULL,
    cover_url VARCHAR,
    description TEXT,
    category_id UUID REFERENCES categories(id) ON DELETE SET NULL,
    provider_id UUID REFERENCES providers(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT NOW() NOT NULL
);

CREATE TRIGGER update_podcasts_updated_at BEFORE UPDATE ON podcasts 
FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Enable RLS
ALTER TABLE podcasts ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Podcasts are viewable by everyone" ON podcasts
    FOR SELECT USING (true);

---------------------------------------------------------
-- 6. PODCAST EPISODES / TRACKS TABLE
---------------------------------------------------------
CREATE TABLE IF NOT EXISTS podcast_episodes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    podcast_id UUID REFERENCES podcasts(id) ON DELETE CASCADE,
    speaker_id UUID REFERENCES speakers(id) ON DELETE SET NULL,
    title VARCHAR NOT NULL,
    duration_string VARCHAR NOT NULL,
    cover_url VARCHAR,
    genre VARCHAR,
    play_count INT DEFAULT 0 NOT NULL,
    description TEXT NOT NULL,
    audio_url VARCHAR NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT NOW() NOT NULL
);

CREATE TRIGGER update_podcast_episodes_updated_at BEFORE UPDATE ON podcast_episodes 
FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Enable RLS
ALTER TABLE podcast_episodes ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Episodes are viewable by everyone" ON podcast_episodes
    FOR SELECT USING (true);

---------------------------------------------------------
-- 7. MESSAGES TABLE (AI Assistant Chat Logs)
---------------------------------------------------------
CREATE TABLE IF NOT EXISTS messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    sender VARCHAR NOT NULL, -- 'user' or 'assistant'
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL
);

-- Enable RLS
ALTER TABLE messages ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can only read and manage their own messages" ON messages
    FOR ALL USING (auth.uid() = user_id);

---------------------------------------------------------
-- 8. COLLECTIONS TABLE (Playlists / Groupings)
---------------------------------------------------------
CREATE TABLE IF NOT EXISTS collections (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR NOT NULL,
    description TEXT,
    is_public BOOLEAN DEFAULT FALSE NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT NOW() NOT NULL
);

CREATE TRIGGER update_collections_updated_at BEFORE UPDATE ON collections 
FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Enable RLS
ALTER TABLE collections ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Public collections are viewable by everyone, private ones only by owner" ON collections
    FOR SELECT USING (is_public = true OR auth.uid() = user_id);

CREATE POLICY "Users can manage their own collections" ON collections
    FOR ALL USING (auth.uid() = user_id);

---------------------------------------------------------
-- 9. COLLECTION ITEMS TABLE (Many-to-Many junction)
---------------------------------------------------------
CREATE TABLE IF NOT EXISTS collection_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    collection_id UUID NOT NULL REFERENCES collections(id) ON DELETE CASCADE,
    episode_id UUID NOT NULL REFERENCES podcast_episodes(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    UNIQUE(collection_id, episode_id)
);

-- Enable RLS
ALTER TABLE collection_items ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Collection items are viewable if the parent collection is viewable" ON collection_items
    FOR SELECT USING (
        EXISTS (
            SELECT 1 FROM collections 
            WHERE collections.id = collection_id 
            AND (collections.is_public = true OR collections.user_id = auth.uid())
        )
    );

CREATE POLICY "Users can manage items in their own collections" ON collection_items
    FOR ALL USING (
        EXISTS (
            SELECT 1 FROM collections 
            WHERE collections.id = collection_id 
            AND collections.user_id = auth.uid()
        )
    );

---------------------------------------------------------
-- 10. FAVORITES TABLE
---------------------------------------------------------
CREATE TABLE IF NOT EXISTS favorites (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    episode_id UUID NOT NULL REFERENCES podcast_episodes(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    UNIQUE(user_id, episode_id)
);

-- Enable RLS
ALTER TABLE favorites ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can manage their own favorites" ON favorites
    FOR ALL USING (auth.uid() = user_id);

---------------------------------------------------------
-- 11. LISTENING PROGRESS TABLE (Cloud sync ready)
---------------------------------------------------------
CREATE TABLE IF NOT EXISTS listening_progress (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    episode_id UUID NOT NULL REFERENCES podcast_episodes(id) ON DELETE CASCADE,
    last_position_seconds INT DEFAULT 0 NOT NULL,
    duration_seconds INT DEFAULT 0 NOT NULL,
    completed BOOLEAN DEFAULT FALSE NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    UNIQUE(user_id, episode_id)
);

CREATE TRIGGER update_listening_progress_updated_at BEFORE UPDATE ON listening_progress 
FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Enable RLS
ALTER TABLE listening_progress ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can manage their own listening progress" ON listening_progress
    FOR ALL USING (auth.uid() = user_id);

---------------------------------------------------------
-- 12. COMPLETED HISTORY TABLE
---------------------------------------------------------
CREATE TABLE IF NOT EXISTS completed_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    episode_id UUID NOT NULL REFERENCES podcast_episodes(id) ON DELETE CASCADE,
    completed_at TIMESTAMPTZ DEFAULT NOW() NOT NULL
);

-- Enable RLS
ALTER TABLE completed_history ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view and manage their own completed history" ON completed_history
    FOR ALL USING (auth.uid() = user_id);

---------------------------------------------------------
-- 13. NOTES TABLE (Interactive user annotations)
---------------------------------------------------------
CREATE TABLE IF NOT EXISTS notes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    episode_id UUID NOT NULL REFERENCES podcast_episodes(id) ON DELETE CASCADE,
    note_text TEXT NOT NULL,
    timestamp_seconds INT, -- Timestamp reference inside audio
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT NOW() NOT NULL
);

CREATE TRIGGER update_notes_updated_at BEFORE UPDATE ON notes 
FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Enable RLS
ALTER TABLE notes ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view and manage their own notes" ON notes
    FOR ALL USING (auth.uid() = user_id);

---------------------------------------------------------
-- 14. INSIGHTS TABLE (AI summaries and takeaways)
---------------------------------------------------------
CREATE TABLE IF NOT EXISTS insights (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    episode_id UUID UNIQUE NOT NULL REFERENCES podcast_episodes(id) ON DELETE CASCADE,
    summary TEXT NOT NULL,
    key_takeaways JSONB DEFAULT '[]'::jsonb NOT NULL,
    generated_at TIMESTAMPTZ DEFAULT NOW() NOT NULL
);

-- Enable RLS
ALTER TABLE insights ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Insights are public and viewable by everyone" ON insights
    FOR SELECT USING (true);

---------------------------------------------------------
-- 15. SEARCH HISTORY TABLE
---------------------------------------------------------
CREATE TABLE IF NOT EXISTS search_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    query VARCHAR NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL
);

-- Enable RLS
ALTER TABLE search_history ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can manage their own search history" ON search_history
    FOR ALL USING (auth.uid() = user_id);

---------------------------------------------------------
-- 16. DOWNLOADS TABLE (Metadata-only tracking)
---------------------------------------------------------
CREATE TABLE IF NOT EXISTS downloads (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    episode_id UUID NOT NULL REFERENCES podcast_episodes(id) ON DELETE CASCADE,
    download_status VARCHAR DEFAULT 'pending' NOT NULL, -- 'pending', 'downloading', 'completed', 'failed'
    file_path VARCHAR,
    file_size_bytes BIGINT,
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    UNIQUE(user_id, episode_id)
);

CREATE TRIGGER update_downloads_updated_at BEFORE UPDATE ON downloads 
FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Enable RLS
ALTER TABLE downloads ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can manage their own downloads metadata" ON downloads
    FOR ALL USING (auth.uid() = user_id);


---------------------------------------------------------
-- HIGHLY OPTIMIZED INDEXES FOR ACCELERATED QUERIES
---------------------------------------------------------

-- 1. Full-text search and filtering optimization
CREATE INDEX IF NOT EXISTS idx_episodes_podcast_id ON podcast_episodes(podcast_id);
CREATE INDEX IF NOT EXISTS idx_episodes_speaker_id ON podcast_episodes(speaker_id);
CREATE INDEX IF NOT EXISTS idx_podcasts_category_id ON podcasts(category_id);
CREATE INDEX IF NOT EXISTS idx_podcasts_provider_id ON podcasts(provider_id);

-- 2. Performance indexing for Search query matches (Gin-trgm for partial matching)
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE INDEX IF NOT EXISTS idx_episodes_title_trgm ON podcast_episodes USING gin (title gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_speakers_name_trgm ON speakers USING gin (name gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_podcasts_title_trgm ON podcasts USING gin (title gin_trgm_ops);

-- 3. Speed index for user interactions
CREATE INDEX IF NOT EXISTS idx_favorites_user_id ON favorites(user_id);
CREATE INDEX IF NOT EXISTS idx_progress_user_id ON listening_progress(user_id);
CREATE INDEX IF NOT EXISTS idx_notes_user_id ON notes(user_id);
CREATE INDEX IF NOT EXISTS idx_notes_episode_id ON notes(episode_id);
CREATE INDEX IF NOT EXISTS idx_messages_user_id ON messages(user_id);
CREATE INDEX IF NOT EXISTS idx_downloads_user_id ON downloads(user_id);
CREATE INDEX IF NOT EXISTS idx_search_history_user_id ON search_history(user_id);
