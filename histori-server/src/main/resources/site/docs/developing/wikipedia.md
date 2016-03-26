# Working with Wikipedia Archives

histori.city offers a number of command-line tools for importing data from Wikipedia.

To use these tools, first download an archive copy of the English language Wikipedia, available from the Wikipedia website.

## Preparing a disk

To be safe, have a filesystem with 300GB of space and 100M inodes. Mount the device for maximum speed.
For Linux, these commands are good if your filesystem is at least 250GB in size:

    mke2fs -t ext4 -i 2048 -b 1024 /dev/your_device_name
    mount -o noatime,data=writeback,barrier=0,nobh,errors=remount-ro /dev/your_device_name /mnt/path
 
## Indexing

Once you have downloaded the Wikipedia archive, uncompress it. It will be large (about 55GB) but further processing will be much faster.

Now that it's uncompressed, you'll need to index it. This creates one file per Wikipedia article. You will need about 300GB of disk space.

To index it, create a directory to contain the index. This directory should be on a filesystem with at least 300GB of free space and 
at least 40 million free inodes.

    cat uncompressed-wikipedia-archive.xml | ./run.sh index --wiki-dir /path/to/wiki/index

Now /path/to/wiki/index contains your own private copy of Wikipedia!

The above command to create the index will take many hours (even days) to complete. If you want things to go faster, consider
running multiple commands in parallel, using the `-s`/`--skip-lines` and `-S`/`--stop-lines` options. The dumpfile above
has lines. You can divide by the number of parallel jobs, and start each job at a multiple of that line count. The indexer will
stop after it has completed the last article that begins before the line number specified by `-S`/`--stop-lines`. This way, you
will not lose any articles if they cross a boundary.

## Filtering

To determine which articles you might want to import, you'll filter the archive to find articles that match certain patterns.

For example, to find all battles, run:

    cat uncompressed-wikipedia-archive.xml | ./run.sh index \
        --wiki-dir /path/to/wiki/index \
        --filter histori.wiki.linematcher.MilitaryConflictInfoboxLineMatcher \
        --filter-log battles.txt

This command will read the entire Wikipedia archive and output article titles to 'battles.txt' that are flagged by the `MilitaryConflictInfoboxLineMatcher`.

This matcher flags any article that contains `{{infobox military conflict`

To implement your own matchers, create a class that extends `histori.wiki.linematcher.LineMatcher` or reuse an existing `LineMatcher` 
implementation, like `RegexLineMatcher`, for example:

    cat uncompressed-wikipedia-archive.xml | ./run.sh index \
        --wiki-dir /path/to/wiki/index \
        --filter histori.wiki.linematcher.RegexLineMatcher \
        --filter-args 'regex-goes-here'
        --filter-log matched-titles.txt

## Nexus Creation

Now that you have some article titles to work with, you can create a Nexus from them.

This is (currently) a bit more difficult. You will need to create a Java class that implements the `WikiDataFinder` interface.
Take a look at `histori.wiki.finder.impl.ConflictFinder` and `histori.wiki.finder.impl.MetroFinder` to see some examples.

After you've created your Finder, update `histori.wiki.finder.FinderFactory` so that it will use your new Finder when examining the articles
you've selected.

Create a directory to hold the created nexuses, for example `/path/to/nexus`

Now let's run the nexus creator:

    cat matched-titles.txt | ./run.sh nexus \
      --wiki-dir /path/to/wiki/index \
      --output-dir /path/to/nexus

The nexus creator will read each title from `matched-titles.txt`, parse the article from the wiki index, and attempt to create a nexus JSON file from it.
If it succeeds, the nexus JSON output file will be placed in a subdirectory under /path/to/nexus

## Importing to histori

All the previous activities (indexing, filtering, nexus creation) can be done without any running server.

To import nexuses into histori, you will need a running histori server.
TBD: Add instructions on setting up and running a histori server.
 
With your histori server running, run this script:

    HISTORI_PASS=your_password API_ACCOUNT=user@domain.com ./run.sh import -i /path/to/nexus

If the account named by `API_ACCOUNT` does not exist, it will be created with the password given.
