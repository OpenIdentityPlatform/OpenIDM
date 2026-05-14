DROP index idx_links_first on [openidm].[links];
DROP index idx_links_second on [openidm].[links];

ALTER TABLE [openidm].[links] ALTER COLUMN linktype NVARCHAR(50);
ALTER TABLE [openidm].[links] ALTER COLUMN linkqualifier NVARCHAR(50);

CREATE UNIQUE INDEX idx_links_first ON [openidm].[links] (linktype ASC, linkqualifier ASC, firstid ASC);
CREATE UNIQUE INDEX idx_links_second ON [openidm].[links] (linktype ASC, linkqualifier ASC, secondid ASC);
