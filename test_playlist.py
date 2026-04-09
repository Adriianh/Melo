import urllib.request, json
import ssl
def fetch_playlist(browseId):
    url = "https://music.youtube.com/youtubei/v1/browse"
    payload = {
        "context": {
            "client": {
                "clientName": "WEB_REMIX",
                "clientVersion": "1.20251227.01.00",
            }
        },
        "browseId": browseId
    }
    req = urllib.request.Request(url, data=json.dumps(payload).encode('utf-8'))
    req.add_header("Content-Type", "application/json")
    try:
        ctx = ssl._create_unverified_context()
        res = urllib.request.urlopen(req, context=ctx).read()
        j = json.loads(res.decode('utf-8'))
        with open("dump_test_playlist.json", "w") as f:
            json.dump(j, f, indent=2)
        print("Playlist dumped.")
    except Exception as e:
        print("ERROR", e)
fetch_playlist("VLPLOU2XLYxmsIJhZgJt9t4wD-UXXi9q_Wqj")
# using a random playlist 
