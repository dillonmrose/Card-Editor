import urllib2
from HTMLParser import HTMLParser
from time import sleep
import urllib

class CardListHTMLParser(HTMLParser):
  def parse(self, html):
      self.card_urls = []
      self.feed(html)
      return self.card_urls

  def handle_starttag(self, tag, attrs):
    if tag.lower() == 'a':
      for attr in attrs:
        if attr[0].lower() == 'href':
          if 'http://gatherer.wizards.com/Pages/Card/Details.aspx?name=' in attr[1]:
            self.card_urls.append(attr[1])


#<td id="ctl00_ctl00_ctl00_MainContent_SubContent_SubContent_rightCol" class="rightCol">
class CardInfoHTMLParser(HTMLParser):
  tableFlag = 0
  attrFlag = 0
  stack = []
  data = {}
  currentAttr = ''
  colors = ['White', 'Blue', 'Black', 'Green', 'Red']

  def parse(self, html):
    self.data = {'Color':'Colorless'}
    self.feed(html)
    return self.data

  def handle_starttag(self, tag, attrs):
    for attr in attrs:
      if attr[0].lower() == 'id':
        if 'ctl00_ctl00_ctl00_MainContent_SubContent_SubContent_' in attr[1] and 'Row' in attr[1]:
          if self.tableFlag:
            self.data[attr[1]] = ''
            self.currentAttr = attr[1]
            self.attrFlag = 1
        if 'ctl00_ctl00_ctl00_MainContent_SubContent_SubContent_rightCol' in attr[1]:
          self.tableFlag = 1

      if attr[0].lower() == 'alt':
        for color in self.colors:
          if color in attr[1]:
            self.data['Color'] = color

      if attr[0].lower() == 'src':
        if '../../Handlers/Image.ashx?multiverseid=' in attr[1]:
          self.data['Image'] = attr[1]

    if self.attrFlag:
      self.stack.append(tag)

  def handle_data(self, data):
    if self.tableFlag and self.attrFlag:
      self.data[self.currentAttr] += data.strip().decode("ascii", "ignore")

  def handle_endtag(self, tag):
    if 'td' in tag:
      self.tableFlag = 0
    if len(self.stack) > 0:
      self.stack.pop()
    if len(self.stack) == 0:
      attrFlag = 0

def get_card_urls(url):
  response = urllib2.urlopen(url)
  html = response.read()
  parser = CardListHTMLParser()
  return parser.parse(html)

def get_card_info(card_url):
  response = urllib2.urlopen(card_url)
  html = response.read()
  parser = CardInfoHTMLParser()
  return parser.parse(html)

data_types = ['ctl00_ctl00_ctl00_MainContent_SubContent_SubContent_numberRow',
              'ctl00_ctl00_ctl00_MainContent_SubContent_SubContent_nameRow',
              'ctl00_ctl00_ctl00_MainContent_SubContent_SubContent_textRow',
              'ctl00_ctl00_ctl00_MainContent_SubContent_SubContent_flavorRow',
              'ctl00_ctl00_ctl00_MainContent_SubContent_SubContent_manaRow',
              'ctl00_ctl00_ctl00_MainContent_SubContent_SubContent_typeRow',
              'ctl00_ctl00_ctl00_MainContent_SubContent_SubContent_otherSetsRow',
              'ctl00_ctl00_ctl00_MainContent_SubContent_SubContent_cmcRow',
              'ctl00_ctl00_ctl00_MainContent_SubContent_SubContent_rarityRow',
              'ctl00_ctl00_ctl00_MainContent_SubContent_SubContent_setRow',
              'ctl00_ctl00_ctl00_MainContent_SubContent_SubContent_artistRow',
              'ctl00_ctl00_ctl00_MainContent_SubContent_SubContent_ptRow',
              'ctl00_ctl00_ctl00_MainContent_SubContent_SubContent_markRow',
              'ctl00_ctl00_ctl00_MainContent_SubContent_SubContent_colorIndicatorRow']

output_file = open('LegacyCube.2015-09-14.tsv', 'w')
card_urls = get_card_urls("http://magic.wizards.com/en/articles/archive/legacy-cube-cardlist-2015-09-14#")

output_file.write('card_url\tcolor')
for data_type in data_types:
  output_file.write('\t' + data_type[52:len(data_type) - 3])
output_file.write('\timage_url\timage_file\tbooze_text\n')


counter = 0
for card_url in card_urls:
  print card_url
  card_info = {}
  try:
    card_info = get_card_info(card_url)
  except:
    pass
  output_file.write(card_url + '\t')

  if 'Color' in card_info:
    output_file.write(card_info['Color'])

  for data_type in data_types:
    output_file.write('\t')
    if data_type in card_info:
      output_file.write(card_info[data_type][card_info[data_type].find(':') + 1:])

  image_file = str(counter) + '.jpg'
  counter = counter + 1

  output_file.write('\t')
  if 'Image' in card_info:
    output_file.write(card_info['Image'])

    output_file.write('\t' + image_file + '\n')

    image_url = card_info['Image']
    image_url = 'http://gatherer.wizards.com' + image_url[5:]
    urllib.urlretrieve(image_url, 'Cards/Base/' + image_file)
  else:
    output_file.write('\t\t\n')

output_file.close()
