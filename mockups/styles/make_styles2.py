# -*- coding: utf-8 -*-
"""
「Accounts」风格 Demo 生成器（第二轮·修订4）
- D 多巴胺 / F 印刻：保持原样
- E 晨雾：按用户意见持续细调（无文字提示、入口移至统计页、布局舒展、月份跳转选择、底栏间距加大）
运行: python make_styles2.py
"""
import os

# ---------- 基础样式 ----------
BASE_CSS = r'''
*{box-sizing:border-box; margin:0; padding:0;}
body{font-family:system-ui,-apple-system,"PingFang SC","Microsoft YaHei",sans-serif;
     background:var(--page); color:var(--ink); padding:30px 16px 64px;}
h1{font-size:23px; text-align:center; font-weight:800;}
.sub{text-align:center; font-size:13px; color:var(--ink2); margin:8px auto 4px; max-width:840px; line-height:1.75;}
.palette{display:flex; justify-content:center; align-items:center; gap:8px; margin:12px auto 2px; flex-wrap:wrap;}
.palette .sw{width:22px; height:22px; border-radius:7px; display:inline-block; box-shadow:inset 0 0 0 1px rgba(128,128,128,.25);}
.palette .swtxt{font-size:11px; color:var(--ink2); font-variant-numeric:tabular-nums;}
.traits{list-style:none; display:flex; flex-wrap:wrap; gap:8px; justify-content:center; margin:10px auto 0; max-width:880px;}
.traits li{font-size:12px; background:var(--accent-soft); border:1px solid var(--line);
           padding:4px 12px; border-radius:999px;}
.back{text-align:center; margin-top:12px; font-size:12.5px;}
.back a{color:var(--accent); text-decoration:none; font-weight:600;}
.stage{display:flex; flex-wrap:wrap; gap:34px; justify-content:center; align-items:flex-start; margin-top:26px;}
.col{display:flex; flex-direction:column; align-items:center; gap:12px;}
.caption{max-width:340px; font-size:12.5px; color:#555; line-height:1.7; background:var(--card);
         border:1px solid var(--card-border); border-radius:12px; padding:10px 12px;}
.caption b{color:var(--ink);}
.phone{width:340px; background:var(--frame); border-radius:46px; padding:10px; box-shadow:0 12px 34px rgba(10,15,25,.25);}
.screen{background:var(--screen); border-radius:36px; overflow:hidden; height:700px; display:flex; flex-direction:column;}
.statusbar{display:flex; justify-content:space-between; align-items:center; padding:10px 22px 6px; font-size:11px;
           background:var(--screen); color:var(--ink2);}
.content{flex:1; overflow:hidden; display:flex; flex-direction:column;}
.scroll{flex:1; overflow:hidden; padding:2px 14px 0;}
.nav{display:flex; border-top:1px solid var(--line); background:var(--screen); padding:6px 0 10px;}
.nav .item{flex:1; text-align:center; font-size:10px; color:var(--ink2);}
.nav .item .ic{font-size:19px; display:block; margin-bottom:1px; line-height:1;}
.nav .item.on{color:var(--accent); font-weight:700;}
.seg{display:flex; gap:0; background:var(--accent-soft); border-radius:12px; margin:10px 16px 2px; padding:3px;}
.seg span{flex:1; text-align:center; padding:7px 0; border-radius:9px; font-size:13px; color:var(--ink2);}
.seg span.on{background:linear-gradient(90deg,var(--accent),var(--accent2)); color:#fff; font-weight:700;}
.amount{text-align:center; font-size:44px; font-weight:700; font-variant-numeric:tabular-nums; letter-spacing:1px;
        padding:12px 0 2px; color:var(--ink);}
.amount small{font-size:22px; font-weight:500; color:var(--ink2); margin-right:6px;}
.noteinput{text-align:center; color:var(--ink2); font-size:13px; padding:4px 0 10px; opacity:.75;}
.sec{font-size:11.5px; color:var(--ink2); padding:12px 4px 8px; letter-spacing:.5px;}
.catgrid{display:grid; grid-template-columns:repeat(4,1fr); gap:8px;}
.cat{display:flex; flex-direction:column; align-items:center; gap:3px; padding:9px 0 7px;
     border:1px solid var(--line); border-radius:14px; font-size:10.5px; background:var(--card); color:var(--ink);}
.cat .e{font-size:20px; line-height:1;}
.meta{display:flex; justify-content:space-between; font-size:12px; padding:12px 4px 0; color:var(--ink);}
.meta .sub{color:var(--ink2); font-size:12px;}
.chips{display:flex; gap:6px; flex-wrap:wrap; padding:8px 0 2px;}
.chip{font-size:12px; padding:5px 12px; border-radius:999px; border:1px solid var(--line); background:var(--card); color:var(--ink);}
.chip.star{background:#FFF7E6; border-color:#F0D59A;}
.save{margin:12px 0 14px; background:linear-gradient(90deg,var(--accent),var(--accent2)); color:#fff;
      text-align:center; padding:13px 0; border-radius:14px; font-size:15px; font-weight:700;}
.barhead{display:flex; align-items:center; justify-content:space-between; padding:4px 16px 2px;}
.barhead h2{font-size:17px; color:var(--ink);}
.month{font-size:13px; color:var(--accent); font-weight:700; letter-spacing:2px;}
.agg{display:flex; gap:8px; padding:8px 16px 6px;}
.agg .box{flex:1; background:var(--card); border:1px solid var(--card-border); border-radius:12px; padding:8px 10px;}
.agg .box .t{font-size:10.5px; color:var(--ink2);}
.agg .box .v{font-size:15px; font-weight:700; font-variant-numeric:tabular-nums; margin-top:2px; color:var(--ink);}
.search{margin:8px 16px 4px; background:var(--accent-soft); border-radius:10px; padding:8px 12px;
        font-size:12.5px; color:var(--ink2);}
.day{display:flex; justify-content:space-between; font-size:11px; color:var(--ink2);
     padding:10px 6px 6px; border-top:1px solid var(--line); margin-top:8px;}
.day .s{color:var(--ink); font-weight:700;}
.row{display:flex; align-items:center; gap:10px; padding:7px 6px;}
.tile{width:38px; height:38px; border-radius:12px; display:flex; align-items:center; justify-content:center;
      font-size:16px; flex:none; background:var(--accent-soft);}
.txt{flex:1; min-width:0;}
.txt .n{font-size:13.5px; color:var(--ink);}
.txt .d{font-size:11px; color:var(--ink2); margin-top:1px; white-space:nowrap; overflow:hidden; text-overflow:ellipsis;}
.amt{font-size:14px; font-weight:700; font-variant-numeric:tabular-nums;}
.amt.minus{color:var(--expense);}
.amt.plus{color:var(--income);}
.topline{display:flex; justify-content:space-between; align-items:center; padding:4px 16px 2px;}
.topline h2{font-size:17px; color:var(--ink);}
.pill{font-size:12.5px; color:var(--accent); border:1px solid var(--accent); border-radius:999px; padding:4px 12px;}
.cards{display:flex; gap:8px; padding:10px 16px 4px;}
.card{flex:1; background:var(--card); border:1px solid var(--card-border); border-radius:14px; padding:10px 12px;}
.card .t{font-size:11px; color:var(--ink2);}
.card .v{font-size:17px; font-weight:800; font-variant-numeric:tabular-nums; margin-top:3px; color:var(--ink);}
.card .d{font-size:11px; margin-top:2px;}
.down{color:var(--expense);} .up{color:var(--income);}
.ringwrap{display:flex; align-items:center; gap:16px; padding:12px 18px 6px;}
.donut{width:118px; height:118px; border-radius:50%; flex:none; position:relative;
  background:conic-gradient(var(--c-food) 0 35%, var(--c-rent) 35% 86%, var(--c-shop) 86% 94%, var(--c-travel) 94% 100%);}
.donut::after{content:""; position:absolute; inset:26px; background:var(--screen); border-radius:50%;}
.donut .ct{position:absolute; inset:0; display:flex; flex-direction:column; align-items:center;
           justify-content:center; z-index:1;}
.donut .ct .a{font-size:15px; font-weight:800; color:var(--ink);}
.donut .ct .b{font-size:10px; color:var(--ink2);}
.legend{flex:1; font-size:12px; display:flex; flex-direction:column; gap:6px; color:var(--ink);}
.legend div{display:flex; align-items:center; gap:6px;}
.dot{width:9px; height:9px; border-radius:3px; flex:none;}
.legend .pct{margin-left:auto; color:var(--ink2); font-variant-numeric:tabular-nums;}
.rank{padding:4px 18px 6px;}
.rank .r{display:flex; align-items:center; gap:8px; padding:6px 0;}
.rank .rn{width:26px; font-size:11.5px; color:var(--ink);}
.rank .rt{width:24px; text-align:center;}
.track{flex:1; height:14px; background:var(--accent-soft); border-radius:7px; overflow:hidden;}
.fill{height:100%; border-radius:7px;}
.rank .rv{width:74px; text-align:right; font-size:12px; font-variant-numeric:tabular-nums; font-weight:700; color:var(--ink);}
.rank .rp{width:36px; text-align:right; font-size:11px; color:var(--ink2);}
.trendhead{font-size:11.5px; color:var(--ink2); padding:12px 18px 6px;}
.bars{display:flex; align-items:flex-end; gap:10px; height:86px; padding:0 18px 2px;}
.bars .b{flex:1; display:flex; flex-direction:column; align-items:center; gap:4px; height:100%; justify-content:flex-end;}
.bars .v{width:100%; border-radius:6px 6px 0 0; background:var(--accent);}
.bars .l{font-size:10px; color:var(--ink2);}
'''

def mix(hexs, f_white):
    h = hexs.lstrip('#')
    r, g, b = (int(h[i:i+2], 16) for i in (0, 2, 4))
    w = f_white
    return '#%02X%02X%02X' % (int(r + (255 - r) * w), int(g + (255 - g) * w), int(b + (255 - b) * w))

def svg_icon(kind):
    return {
        'plus':  '<svg viewBox="0 0 24 24" width="21" height="21" fill="none"><path d="M12 5.5v13M5.5 12h13" stroke="currentColor" stroke-width="2.3" stroke-linecap="round"/></svg>',
        'list':  '<svg viewBox="0 0 24 24" width="21" height="21" fill="none"><path d="M5 6.8h14M5 12h14M5 17.2h9" stroke="currentColor" stroke-width="2.1" stroke-linecap="round"/></svg>',
        'chart': '<svg viewBox="0 0 24 24" width="21" height="21" fill="none"><path d="M6 20v-9M12 20V4.5M18 20v-6M3.5 20.5h17" stroke="currentColor" stroke-width="2.1" stroke-linecap="round"/></svg>',
        'gear':  '<svg viewBox="0 0 24 24" width="18" height="18" fill="none"><path d="M4 6.5h16M4 12h16M4 17.5h16" stroke="currentColor" stroke-width="2" stroke-linecap="round"/><circle cx="11" cy="6.5" r="2.2" fill="currentColor"/><circle cx="15.5" cy="12" r="2.2" fill="currentColor"/><circle cx="8" cy="17.5" r="2.2" fill="currentColor"/></svg>',
        'chev-r': '<svg viewBox="0 0 24 24" width="15" height="15" fill="none"><path d="M9 5.5l6.5 6.5L9 18.5" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"/></svg>',
        'chev-l': '<svg viewBox="0 0 24 24" width="16" height="16" fill="none"><path d="M15 5.5L8.5 12l6.5 6.5" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"/></svg>',
        'plus-sm': '<svg viewBox="0 0 24 24" width="15" height="15" fill="none"><path d="M12 5v14M5 12h14" stroke="currentColor" stroke-width="2.4" stroke-linecap="round"/></svg>',
        'grip': '<svg viewBox="0 0 24 24" width="15" height="15" fill="currentColor"><circle cx="9" cy="6" r="1.4"/><circle cx="15" cy="6" r="1.4"/><circle cx="9" cy="12" r="1.4"/><circle cx="15" cy="12" r="1.4"/><circle cx="9" cy="18" r="1.4"/><circle cx="15" cy="18" r="1.4"/></svg>',
        'sun': '<svg viewBox="0 0 24 24" width="19" height="19" fill="none"><circle cx="12" cy="12" r="3.6" stroke="currentColor" stroke-width="2"/><path d="M12 2.8v2.4M12 18.8v2.4M2.8 12h2.4M18.8 12h2.4M5.7 5.7l1.7 1.7M16.6 16.6l1.7 1.7M18.3 5.7l-1.7 1.7M7.4 16.6l-1.7 1.7" stroke="currentColor" stroke-width="2" stroke-linecap="round"/></svg>',
        'moon': '<svg viewBox="0 0 24 24" width="19" height="19" fill="none"><path d="M20.6 13.6A8.5 8.5 0 1 1 10.4 3.4a6.8 6.8 0 0 0 10.2 10.2z" stroke="currentColor" stroke-width="2" stroke-linejoin="round"/></svg>',
        'auto': '<svg viewBox="0 0 24 24" width="19" height="19" fill="none"><circle cx="12" cy="12" r="8.4" stroke="currentColor" stroke-width="2"/><path d="M12 3.6a8.4 8.4 0 0 1 0 16.8z" fill="currentColor"/></svg>',
    }.get(kind, kind)

def nav_html(kind):
    ics = [svg_icon('plus'), svg_icon('list'), svg_icon('chart')]
    labs = ['记一笔', '明细', '统计']
    on = [1, 0, 0]
    parts = []
    for i in range(3):
        parts.append('<div class="%s"><span class="ic">%s</span>%s</div>'
                     % ('item on' if on[i] else 'item', ics[i], labs[i]))
    return ''.join(parts)

def refined(t):
    return t['nav'] == 'bubbles'

# ---------- 三屏构建 ----------
def phone1(t):
    gnames = ['餐饮', '交通', '购物', '居住', '日用', '娱乐', '医疗', '人情']
    cells = []
    for i, name in enumerate(gnames):
        bg, fg, glyph = t['g'][i]
        cls = 'cat hot' if i == 0 else 'cat'
        cells.append('<div class="%s" style="background:%s;color:%s"><span class="e">%s</span>%s</div>'
                     % (cls, bg, fg, glyph, name))
    if refined(t):
        # E：无任何文字提示；功能入口集中到统计页 ⚙
        seg = '<div class="seg"><span class="on">支出</span><span>收入</span></div>'
        head_cat = ''
        meta = ('<div class="meta"><span>钱包：<span class="sub">现金 ▾</span></span>'
                '<span>今天 12:30 ▾</span></div>')
        head_recent = '<div class="sec">最近金额</div>'
        head_tpl = '<div class="sec">我的模板</div>'
        add_btn = '<span class="chip add">＋</span>'
    else:
        seg = '<div class="seg"><span class="on">支出</span><span>收入</span></div>'
        head_cat = '<div class="sec">选个分类 · 点选即记好</div>'
        meta = ('<div class="meta"><span>💼 钱包：<span class="sub">现金 ▾</span></span>'
                '<span>🕐 今天 12:30 ▾</span></div>')
        head_recent = '<div class="sec">最近金额</div>'
        head_tpl = '<div class="sec">我的模板</div>'
        add_btn = '<span class="chip">＋ 新增</span>'
    return ('<div class="phone"><div class="screen">'
            '<div class="statusbar"><span>12:30</span><span style="letter-spacing:3px;font-size:9px;">●●● 89%</span></div>'
            '<div class="content">'
            + seg +
            '<div class="amount"><small>¥</small>0.00</div>'
            '<div class="noteinput">备注（可选）</div>'
            '<div class="scroll">'
            + head_cat +
            '<div class="catgrid">' + ''.join(cells) + '</div>'
            + meta + head_recent +
            '<div class="chips"><span class="chip">15</span><span class="chip">23.5</span><span class="chip">6</span><span class="chip">300</span></div>'
            + head_tpl +
            '<div class="chips"><span class="chip star">⭐ 早餐 ¥8</span><span class="chip star">⭐ 地铁 ¥6</span>' + add_btn + '</div>'
            '<div class="save">保 存</div>'
            '</div></div>'
            '<div class="nav">' + nav_html(t['nav']) + '</div></div></div>')

def phone2(t):
    rows = []
    for (tilebg, tilefg, glyph, n, d, amt, cls) in t['rows']:
        rows.append('<div class="row"><div class="tile" style="background:%s;color:%s">%s</div>'
                    '<div class="txt"><div class="n">%s</div><div class="d">%s</div></div>'
                    '<div class="amt %s">%s</div></div>' % (tilebg, tilefg, glyph, n, d, cls, amt))
    if refined(t):
        search = '<div class="search">搜索备注…　　[筛选 ▾]</div>'
        month = '‹ <span class="monthpick">11月 ▾</span> ›'
    else:
        search = '<div class="search">🔍 搜索备注…　　[筛选 ▾]</div>'
        month = '‹ 11月 ›'
    return ('<div class="phone"><div class="screen">'
            '<div class="statusbar"><span>12:31</span><span style="letter-spacing:3px;font-size:9px;">●●● 89%</span></div>'
            '<div class="content">'
            '<div class="barhead"><h2>明细</h2><span class="month">' + month + '</span></div>'
            '<div class="agg"><div class="box"><div class="t">本月支出</div><div class="v">¥2,345.00</div></div>'
            '<div class="box"><div class="t">本月收入</div><div class="v" style="color:var(--income)">¥9,000.00</div></div></div>'
            + search +
            '<div class="scroll">'
            '<div class="day"><span>11月12日 周二</span><span>支出 <span class="s">¥47.50</span></span></div>'
            + ''.join(rows[:3]) +
            '<div class="day"><span>11月11日 周一</span><span>支出 <span class="s">¥1,200.00</span></span></div>'
            + ''.join(rows[3:]) +
            '</div></div>'
            '<div class="nav">' + nav_html(t['nav']) + '</div></div></div>')

def phone3(t):
    if refined(t):
        top_html = ('<div class="topline"><h2>统计</h2><span class="top-actions">'
                    '<span class="gear2" title="设置">' + svg_icon('gear') + '</span>'
                    '<span class="pill">本月 ▾</span></span></div>')
        rank_rows = [
            ('居住', '51%', '¥1,200.00', 'var(--c-rent)'),
            ('餐饮', '35%', '¥820.00', 'var(--c-food)'),
            ('购物', '8%', '¥180.00', 'var(--c-shop)'),
            ('交通', '6%', '¥145.00', 'var(--c-travel)'),
        ]
        rank_html = []
        for label, pct, amt, color in rank_rows:
            rank_html.append('<div class="r"><span class="rt" style="background:%s"></span>'
                             '<span class="rn">%s</span><div class="track">'
                             '<div class="fill" style="width:%s;background:%s"></div></div>'
                             '<span class="rv">%s</span><span class="rp">%s</span></div>'
                             % (color, label, pct, color, amt, pct))
        rank_section = ('<div class="sec" style="padding:14px 18px 6px">分类排行（支出）</div>'
                        '<div class="rank">' + ''.join(rank_html) + '</div>'
                        '<div class="rank-more">展开全部 12 个分类 ▾</div>')
    else:
        top_html = '<div class="topline"><h2>统计</h2><span class="pill">本月 ▾</span></div>'
        rank_section = ('<div class="sec" style="padding-left:18px">分类排行（支出）· 点击下钻</div>'
                        '<div class="rank">'
                        '<div class="r"><span class="rt">🏠</span><span class="rn">居住</span><div class="track"><div class="fill" style="width:51%;background:var(--c-rent)"></div></div><span class="rv">¥1,200.00</span><span class="rp">51%</span></div>'
                        '<div class="r"><span class="rt">🍜</span><span class="rn">餐饮</span><div class="track"><div class="fill" style="width:35%;background:var(--c-food)"></div></div><span class="rv">¥820.00</span><span class="rp">35%</span></div>'
                        '<div class="r"><span class="rt">🛒</span><span class="rn">购物</span><div class="track"><div class="fill" style="width:8%;background:var(--c-shop)"></div></div><span class="rv">¥180.00</span><span class="rp">8%</span></div>'
                        '<div class="r"><span class="rt">🚇</span><span class="rn">交通</span><div class="track"><div class="fill" style="width:6%;background:var(--c-travel)"></div></div><span class="rv">¥145.00</span><span class="rp">6%</span></div>'
                        '</div>')
    return ('<div class="phone"><div class="screen">'
            '<div class="statusbar"><span>12:32</span><span style="letter-spacing:3px;font-size:9px;">●●● 89%</span></div>'
            '<div class="content">'
            + top_html +
            '<div class="cards">'
            '<div class="card"><div class="t">支出</div><div class="v">¥2,345.00</div><div class="d down">▼ 8% 比上月</div></div>'
            '<div class="card"><div class="t">收入</div><div class="v" style="color:var(--income)">¥9,000.00</div><div class="d up">▲ 12% 比上月</div></div>'
            '<div class="card"><div class="t">结余</div><div class="v">¥6,655.00</div><div class="d" style="color:var(--ink2)">收入−支出</div></div>'
            '</div>'
            '<div class="ringwrap"><div class="donut"><div class="ct"><div class="a">35%</div><div class="b">餐饮</div></div></div>'
            '<div class="legend">'
            '<div><span class="dot" style="background:var(--c-food)"></span>餐饮 <span class="pct">35%</span></div>'
            '<div><span class="dot" style="background:var(--c-rent)"></span>居住 <span class="pct">51%</span></div>'
            '<div><span class="dot" style="background:var(--c-shop)"></span>购物 <span class="pct">8%</span></div>'
            '<div><span class="dot" style="background:var(--c-travel)"></span>交通 <span class="pct">6%</span></div>'
            '</div></div>'
            + rank_section +
            '<div class="trendhead">近 6 个月支出趋势（柱）</div>'
            '<div class="bars">'
            '<div class="b lo"><div class="v" style="height:36%"></div><div class="l">6月</div></div>'
            '<div class="b lo"><div class="v" style="height:52%"></div><div class="l">7月</div></div>'
            '<div class="b lo"><div class="v" style="height:44%"></div><div class="l">8月</div></div>'
            '<div class="b lo"><div class="v" style="height:62%"></div><div class="l">9月</div></div>'
            '<div class="b lo"><div class="v" style="height:55%"></div><div class="l">10月</div></div>'
            '<div class="b hi"><div class="v" style="height:80%"></div><div class="l">11月</div></div>'
            '</div>'
            '</div>'
            '<div class="nav">' + nav_html(t['nav']) + '</div></div></div>')

def phone4(t):
    """E 专属：设置页（由统计页滑杆按钮进入）"""
    chev = svg_icon('chev-r')
    setrow = lambda left, right: ('<div class="setrow"><span class="l">%s</span>'
                                  '<span class="right">%s</span></div>' % (left, right))
    card1 = ('<div class="setcard">' +
             setrow('分类管理', chev) +
             setrow('账户管理', chev) + '</div>')
    card2 = ('<div class="setcard">' +
             setrow('主题', '跟随系统 ' + chev) +
             setrow('默认支出账户', '现金 ' + chev) +
             '<div class="setrow"><span class="l">连续记账</span>'
             '<span class="right"><span class="switch on"><i></i></span></span></div></div>')
    card3 = ('<div class="setcard">' +
             setrow('导出 CSV / JSON', chev) +
             setrow('备份到文件', chev) +
             setrow('从备份恢复', chev) + '</div>')
    card4 = ('<div class="setcard">' +
             setrow('关于 · 版本 v0.1.0', '仅存本机') + '</div>')
    return ('<div class="phone"><div class="screen">'
            '<div class="statusbar"><span>12:33</span><span style="letter-spacing:3px;font-size:9px;">●●● 89%</span></div>'
            '<div class="content">'
            '<div class="sethead"><span class="backbtn">' + svg_icon('chev-l') + '</span>'
            '<span class="settitle">设置</span></div>'
            + card1 + card2 + card3 + card4 +
            '<div class="setfoot">分类管理：增删分类 · 改图标/颜色/名称 · 长按排序（首页固定 8 个）<br>数据仅存本机 · App 无任何网络权限</div>'
            '</div></div></div>')

def page_head(t, title):
    """设置子页通用头部：‹ 返回 + 标题 + 右侧操作"""
    return ('<div class="phone"><div class="screen">'
            '<div class="statusbar"><span>12:34</span><span style="letter-spacing:3px;font-size:9px;">●●● 89%</span></div>'
            '<div class="content">'
            '<div class="sethead"><span class="backbtn">' + svg_icon('chev-l') + '</span>'
            '<span class="settitle">' + title + '</span>'
            '<span style="flex:1"></span><span class="addbtn">' + svg_icon('plus-sm') + '</span></div>')

def page_tail():
    return '</div></div></div>'

def phone5(t):
    """分类管理：支出/收入切换 + 拖拽排序 + 首页标记"""
    items = [('餐饮', '#FF9F68', 'home'), ('交通', '#63A9FF', 'home'), ('购物', '#F07BAF', 'home'),
             ('居住', '#8B7CF6', 'home'), ('日用', '#6FCF97', 'home'), ('娱乐', '#FFB15F', 'home'),
             ('医疗', '#62C6C0', 'home'), ('人情', '#FF8FA3', 'home'),
             ('旅行', '#FFC857', 'extra'), ('学习', '#9FB4FF', 'extra')]
    rows = []
    for name, color, kind in items:
        chip = '<span class="cc" style="background:%s"></span>' % mix(color, .72)
        tag = '<span class="tagp %s">%s</span>' % ('home' if kind == 'home' else 'extra',
                                                   '首页' if kind == 'home' else '仅统计')
        rows.append('<div class="setrow mrow"><span class="grip">' + svg_icon('grip') + '</span>'
                    + chip + '<span class="nm">' + name + '</span>' + tag + '</div>')
    return (page_head(t, '分类管理')
            + '<div class="seg2wrap"><div class="seg2"><span class="on">支出</span><span>收入</span></div></div>'
            + '<div class="setcard">' + ''.join(rows)
            + '<div class="dashrow">' + svg_icon('plus-sm') + '新增分类</div></div>'
            + '<div class="setfoot">拖动手柄排序 · 打"首页"标的 8 个显示在记账首页（首页固定 8 个）<br>'
            '点分类行可改 图标 / 颜色 / 名称 · 收入分类在"收入"页签下管理</div>'
            + page_tail())

def phone6(t):
    """账户管理：账户列表 + 余额 + 转账入口"""
    accounts = [('现金', '#FFB15F', '¥1,250.00'), ('储蓄卡', '#8B7CF6', '¥8,000.00'),
                ('支付宝', '#63A9FF', '¥2,340.50'), ('微信零钱', '#2FC98A', '¥386.20')]
    rows = []
    for name, color, bal in accounts:
        chip = '<span class="cc" style="background:%s"></span>' % mix(color, .72)
        rows.append('<div class="setrow mrow">' + chip + '<span class="nm">' + name + '</span>'
                    '<span class="bal">' + bal + '</span></div>')
    return (page_head(t, '账户管理')
            + '<div class="setcard">' + ''.join(rows)
            + '<div class="dashrow">' + svg_icon('plus-sm') + '新增账户</div></div>'
            + '<div class="setcard"><div class="setrow mrow"><span class="nm">账户转账</span>'
            '<span class="right">转账不计收支 ' + svg_icon('chev-r') + '</span></div></div>'
            + '<div class="setfoot">余额 = 初始余额 + 收入 − 支出 + 转入 − 转出<br>'
            '记账页"钱包"处可随时切换账户或在这里新增</div>'
            + page_tail())

def phone7(t):
    """主题：跟随系统 / 浅色 / 深色"""
    opts = [
        ('auto', '跟随系统', '随手机系统自动切换亮暗（推荐）', True),
        ('sun', '浅色', '始终使用亮色外观', False),
        ('moon', '深色', '始终使用暗色 · 夜间护眼', False),
    ]
    rows = []
    for icon, name, desc, sel in opts:
        rows.append('<div class="themeopt%s"><span class="opt-ic">%s</span>'
                    '<span class="opt-txt"><span class="tt">%s</span><span class="td">%s</span></span>'
                    '<span class="radio"></span></div>'
                    % (' sel' if sel else '', svg_icon(icon), name, desc))
    return (page_head(t, '主题')
            + '<div class="sec" style="padding:2px 4px 8px">外观</div>'
            + '<div class="setcard">' + ''.join(rows) + '</div>'
            + '<div class="setfoot">日 / 夜切换仅在此设置 · 与统计页的滑杆按钮（设置入口）互不混淆</div>'
            + page_tail())

PAGE_HEAD = '''<!doctype html>
<html lang="zh-CN">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>「Accounts」界面 Demo · __NAME__</title>
<style>
__CSS__
</style>
</head>
<body>
<h1>「Accounts」记账 App · 界面风格 Demo</h1>
<p class="sub">风格 <b>__NAME__</b> —— __SUB__</p>
<div class="palette">__PALETTE__</div>
<ul class="traits">__TRAITS__</ul>
<p class="back"><a href="index.html">← 返回风格对比总览</a></p>
<div class="stage">__COLS__</div>
__SUB2__
<footer style="margin-top:40px; text-align:center; font-size:12px; color:var(--ink2);">
  结构与功能完全相同 · 正式版用 Jetpack Compose 实现
</footer>
</body>
</html>'''

DEF_CAPS = {
    'cap1': '<b>记一笔（默认首页）</b><br>打开即记账：输金额 → 点分类即保存；连续记账连录小票；⭐模板一键记账。注意本版的<u>分类图标</u>与<u>底部导航</u>形态。',
    'cap2': '<b>明细</b><br>按日分组 + 当日小计；点一笔可编辑/删除/<b>再次记账</b>；收入绿色＋号；支持搜索筛选。列表行风格跟随主题。',
    'cap3': '<b>统计（默认本月）</b><br>支出/收入/结余 + 环比；环形图 + 分类排行（点分类下钻）；近 6 月趋势。数字由明细实时聚合。',
}

def col(phone_html, cap_html):
    return '<div class="col">%s<div class="caption">%s</div></div>' % (phone_html, cap_html)

def build(t):
    pal = ' '.join('<span class="sw" style="background:%s" title="%s"></span><span class="swtxt">%s</span>'
                   % (h, h, lab) for h, lab in t['palette'])
    traits = ''.join('<li>%s</li>' % x for x in t['traits'])
    css = BASE_CSS + t['css']
    cols = (col(phone1(t), t.get('cap1', DEF_CAPS['cap1']))
            + col(phone2(t), t.get('cap2', DEF_CAPS['cap2']))
            + col(phone3(t), t.get('cap3', DEF_CAPS['cap3'])))
    sub2 = ''
    if refined(t):
        cols += col(phone4(t), t.get('cap4', ''))
        sub2 = ('<div class="subsec">设置 → 点击条目后进入的子页面</div><div class="stage">'
                + col(phone5(t), t.get('cap5', ''))
                + col(phone6(t), t.get('cap6', ''))
                + col(phone7(t), t.get('cap7', '')) + '</div>')
    return (PAGE_HEAD
            .replace('__NAME__', t['name']).replace('__SUB__', t['sub'])
            .replace('__PALETTE__', pal).replace('__TRAITS__', traits)
            .replace('__CSS__', css)
            .replace('__COLS__', cols)
            .replace('__SUB2__', sub2))

# ================= 主题定义 =================
D = dict(
    file='style-d-dopamine.html',
    name='D · 多巴胺（高饱和撞色）',
    sub='高饱和实色 + 汉字徽章图标 + 悬浮胶囊导航：扁平有力、情绪饱满——像一本用色大胆的当代记账杂志，醒目但不幼稚。',
    palette=[('#F5F6F8','亮底'),('#2E5BFF','电光蓝主色'),('#FF5A5F','支出'),('#00C48C','收入'),('#FF7A45','分类橙')],
    traits=['汉字徽章图标（餐/行/购…）','悬浮胶囊导航（滑块式）','高饱和撞色分类块','粗体大金额','扁平少阴影'],
    nav='dock',
    g=[('#FF7A45','#FFFFFF','餐'),('#3E8BFF','#FFFFFF','行'),('#8B5CF6','#FFFFFF','购'),('#F5A623','#FFFFFF','居'),
       ('#00BFA5','#FFFFFF','日'),('#FF5DA2','#FFFFFF','乐'),('#12B886','#FFFFFF','医'),('#E91E63','#FFFFFF','礼')],
    rows=[('#FF7A45','#FFFFFF','餐','午餐','餐饮 · 现金 · 和同事','−23.50','minus'),
          ('#FF9A5C','#FFFFFF','饮','咖啡','餐饮 · 现金','−18.00','minus'),
          ('#3E8BFF','#FFFFFF','行','地铁','交通 · 微信零钱','−6.00','minus'),
          ('#00C48C','#FFFFFF','礼','红包','收入 · 微信零钱','＋200.00','plus'),
          ('#F5A623','#FFFFFF','居','房租','居住 · 储蓄卡 · 11月房租','−1,200.00','minus'),
          ('#8B5CF6','#FFFFFF','购','超市','购物 · 支付宝','−56.80','minus')],
    css=r'''
:root{
  --page:#EEF0F4; --frame:#141926; --screen:#FFFFFF;
  --ink:#171C26; --ink2:#7A8194; --line:#EBEDF2;
  --accent:#2E5BFF; --accent2:#2E5BFF; --accent-soft:#E9EEFF;
  --income:#00C48C; --expense:#FF5A5F;
  --card:#FFFFFF; --card-border:#E4E7EE;
  --c-food:#FF7A45; --c-rent:#F5A623; --c-shop:#8B5CF6; --c-travel:#3E8BFF; --c-income:#00C48C;
}
.amount{font-weight:900;}
.cat{border:none; border-radius:16px; box-shadow:0 3px 8px rgba(23,28,38,.10); font-weight:700;}
.cat .e{font-size:19px; font-weight:800;}
.cat.hot{box-shadow:0 0 0 2px #fff, 0 0 0 5px #2E5BFF, 0 6px 14px rgba(46,91,255,.30);}
.save{border-radius:14px; font-weight:800; box-shadow:0 8px 18px rgba(46,91,255,.30);}
.tile{border:none; border-radius:12px; font-weight:800;}
.phone{box-shadow:0 18px 40px rgba(15,20,35,.35);}
.screen{position:relative;}
.content{padding-bottom:102px;}
.nav{position:absolute; left:10px; right:10px; bottom:10px; border-radius:24px; display:flex;
     background:rgba(255,255,255,.95); backdrop-filter:blur(12px);
     border:1px solid #EDEFF4; box-shadow:0 12px 28px rgba(20,26,40,.16); padding:5px;}
.nav .item{display:flex; flex-direction:column; align-items:center; gap:1px; padding:8px 0 7px;
           border-radius:18px; font-size:10.5px; font-weight:600; color:#7A8194;}
.nav .item.on{background:linear-gradient(135deg,#2E5BFF,#4D8DFF); color:#FFFFFF;
              box-shadow:0 8px 16px rgba(46,91,255,.38); font-weight:800;}
'''
)

E = dict(
    file='style-e-mist.html',
    name='E · 晨雾（浅色玻璃拟态 · 细调版）',
    sub='淡紫·天蓝·薄粉晨雾底 + 磨砂玻璃。按你的全部意见细调：首页零文字提示、设置入口移至统计页"本月"左侧、纵向更舒展、明细可点月份跳年份/日期、底栏三圆间距加大。',
    palette=[('#EDE9FE','晨雾紫'),('#DBEAFE','晨雾蓝'),('#8B7CF6','紫罗兰'),('#5FB4FF','天蓝'),('#2FC98A','收入绿')],
    traits=['首页零文字提示','设置入口（滑杆图标）在统计页"本月"左侧','纵向布局更舒展','月份点按→年/月/日跳转','底栏三圆间距加大','明细行无图标'],
    nav='bubbles',
    g=[(mix('#FF9F68', .80), '#3F4257', ''), (mix('#63A9FF', .80), '#3F4257', ''), (mix('#F07BAF', .80), '#3F4257', ''),
       (mix('#8B7CF6', .80), '#3F4257', ''), (mix('#6FCF97', .80), '#3F4257', ''), (mix('#FFB15F', .80), '#3F4257', ''),
       (mix('#62C6C0', .80), '#3F4257', ''), (mix('#FF8FA3', .80), '#3F4257', '')],
    rows=[('#FF9F68', '#FFFFFF', '', '午餐', '餐饮 · 现金 · 和同事', '−23.50', 'minus'),
          ('#FF9F68', '#FFFFFF', '', '咖啡', '餐饮 · 现金', '−18.00', 'minus'),
          ('#63A9FF', '#FFFFFF', '', '地铁', '交通 · 微信零钱', '−6.00', 'minus'),
          ('#2FC98A', '#FFFFFF', '', '红包', '收入 · 微信零钱', '＋200.00', 'plus'),
          ('#8B7CF6', '#FFFFFF', '', '房租', '居住 · 储蓄卡 · 11月房租', '−1,200.00', 'minus'),
          ('#F472B6', '#FFFFFF', '', '超市', '购物 · 支付宝', '−56.80', 'minus')],
    cap1='<b>记一笔（默认首页）</b><br>① 打开即记账：输金额 → 点分类即保存；② 首页<b>无任何文字提示</b>：分类增删/排序、最近金额按住删除、模板按住修改/删除等能力均在 <b>统计页的设置</b>内与手势中实现；③ 上下留白更舒展。',
    cap2='<b>明细</b><br>① 点击中间月份「11月 ▾」会弹出选择器：可<b>先选年份，再选月份或直接跳转具体日期</b>；两侧 ‹ › 是相邻月份快切；② 搜索无放大镜、记录行无图标。',
    cap3='<b>统计</b><br>① <b>设置按钮（滑杆图标，非日/夜切换）在顶部"本月"的左侧</b>，点击进入 分类管理/账户管理（可改图标、颜色、增删分类）；② 排行超过 4 项可点击"<b>展开全部 12 个分类 ▾</b>"。',
    cap4='<b>设置页（第 4 屏 · 由统计页滑杆按钮进入）</b><br>① <b>分类管理</b>：增删分类、改图标/颜色/名称、长按拖动排序——首页固定的 8 个在这里配置；② <b>账户管理</b>：新增/改名/初始余额、账户间转账；③ 主题（跟随系统/浅色/深色）、默认支出账户、连续记账开关；④ 导出与备份恢复；⑤ 数据仅存本机、无任何网络权限。',
    cap5='<b>分类管理（点设置 → 分类管理进入）</b><br>① 顶部 <b>支出 / 收入</b> 页签切换两套分类；② 拖动左侧手柄<b>排序</b>；③ 打"首页"标的 8 个分类显示在记账首页（固定 8 个），"仅统计"的分类不进首页但仍参与统计；④ 点分类行可改 图标/颜色/名称；⑤ 底部「＋新增分类」。',
    cap6='<b>账户管理（点设置 → 账户管理进入）</b><br>① 账户列表实时显示<b>余额</b>（= 初始 + 收入 − 支出 + 转入 − 转出）；② 「＋新增账户」与记账页"钱包"选择联动；③ 底部「账户转账」入口（转账不计收支，仅影响两账户余额）。',
    cap7='<b>主题（点设置 → 主题进入）</b><br>① <b>跟随系统</b>（默认，推荐）：随手机亮暗自动切换；② 浅色 / 深色 可固定；③ 日 ☀ / 夜 ☾ 图标只在<span style="white-space:nowrap">本页</span>使用，与统计页滑杆设置按钮无歧义；④ 切换即时生效（含统计图表配色）。',
    css=r'''
:root{
  --page:#F0EDF9; --frame:#D8D1E6; --screen:#FFFFFF;
  --ink:#3F4257; --ink2:#9A9DB3; --line:#EEEBF6;
  --accent:#8B7CF6; --accent2:#5FB4FF; --accent-soft:#F2EFFC;
  --income:#2FC98A; --expense:#FF7D9C;
  --card:#FFFFFF; --card-border:#E9E4F5;
  --c-food:#FF9F68; --c-rent:#8B7CF6; --c-shop:#F472B6; --c-travel:#63A9FF; --c-income:#2FC98A;
}
body{background:
  radial-gradient(760px 520px at 8% -6%, rgba(139,124,246,.26), transparent 62%),
  radial-gradient(700px 480px at 96% 4%, rgba(95,180,255,.28), transparent 60%),
  radial-gradient(640px 460px at 50% 108%, rgba(255,143,168,.24), transparent 62%),
  var(--page);}
.phone{border-radius:50px; box-shadow:0 16px 40px rgba(100,85,170,.28);}
/* 纵向更舒展 */
.screen{height:770px;}
.content{padding-top:6px;}
.seg{margin:14px 16px 2px;}
.amount{padding:22px 0 12px;}
.catgrid{column-gap:10px; row-gap:13px; padding-top:6px;}
.cat{border:none!important; border-radius:24px; font-weight:700; padding:18px 0 16px;
     box-shadow:inset 0 0 0 1px rgba(255,255,255,.85), 0 4px 10px rgba(110,100,170,.10);}
.cat .e{display:none;}
.cat.hot{box-shadow:0 0 0 2.5px #8B7CF6, 0 10px 20px rgba(139,124,246,.28);}
.sec{padding:18px 4px 10px; letter-spacing:.5px;}
.meta{margin-top:20px; padding-top:14px; border-top:1px solid rgba(255,255,255,.9);}
.chips{gap:8px 8px; padding:8px 0 4px;}
.save{border-radius:999px; font-weight:800; box-shadow:0 10px 22px rgba(139,124,246,.35);
      margin:26px 0 22px; padding:15px 0;}
.seg span.on{background:linear-gradient(135deg,#8B7CF6,#5FB4FF); box-shadow:0 6px 14px rgba(139,124,246,.35);}
.chip{background:rgba(255,255,255,.66); padding:7px 14px;}
.chip.star{background:#FBF7FF; border-color:#DED4F2;}
.chip.add{width:30px; height:30px; padding:0; border-radius:50%; display:inline-flex; align-items:center;
          justify-content:center; font-size:16px; font-weight:700; color:#8B7CF6;
          background:rgba(255,255,255,.72); border:1.5px dashed rgba(139,124,246,.55);}
/* 明细：无图标 + 月份点选跳转 */
.row .tile{display:none;}
.row{padding:9px 2px;}
.monthpick{display:inline-block; border-bottom:1px dashed #8B7CF6; padding-bottom:1px; cursor:pointer;
           color:#6C5CE7;}
.search,.agg .box,.card{background:rgba(255,255,255,.62); backdrop-filter:blur(10px); border-color:rgba(255,255,255,.95);}
.track{background:rgba(255,255,255,.7);}
/* 统计 */
.rank .rt{width:12px!important; height:12px; border-radius:50%; display:inline-block; font-size:0;}
.rank-more{margin:2px 18px 8px; padding:11px 0; text-align:center; font-size:12px; color:#8B7CF6;
           font-weight:800; background:rgba(255,255,255,.6); border-radius:14px;
           backdrop-filter:blur(8px); border:1px solid rgba(255,255,255,.9);}
.top-actions{display:inline-flex; align-items:center; gap:10px;}
.gear2{width:34px; height:34px; border-radius:50%; display:inline-flex; align-items:center; justify-content:center;
       color:#8E93A8; background:rgba(255,255,255,.74); backdrop-filter:blur(10px);
       border:1px solid rgba(255,255,255,.95); box-shadow:0 3px 8px rgba(110,100,170,.12);}
/* 底部：纯圆导航（间距加大、无文字） */
.screen{position:relative;}
.content{padding-bottom:92px;}
.nav{position:absolute; left:0; right:0; bottom:10px; background:transparent; border-top:none;
     display:flex; justify-content:space-between; align-items:center; padding:0 58px;}
.nav .item{flex:none; display:flex; flex-direction:column; align-items:center; font-size:0; color:#9A9DB3;}
.nav .item .ic{width:46px; height:46px; border-radius:50%; display:flex; align-items:center; justify-content:center;
   background:rgba(255,255,255,.74); backdrop-filter:blur(10px);
   border:1px solid rgba(255,255,255,.95); box-shadow:0 4px 10px rgba(110,100,170,.13); color:#8E93A8;}
.nav .item.on{color:#6C5CE7;}
.nav .item.on .ic{background:linear-gradient(135deg,#8B7CF6,#5FB4FF); color:#FFFFFF;
   box-shadow:0 7px 16px rgba(139,124,246,.42); transform:scale(1.08);}
/* 设置页 */
.sethead{display:flex; align-items:center; gap:8px; padding:16px 12px 14px;}
.backbtn{width:36px; height:36px; border-radius:50%; display:flex; align-items:center; justify-content:center;
   color:#8E93A8; background:rgba(255,255,255,.74); backdrop-filter:blur(10px);
   border:1px solid rgba(255,255,255,.95); box-shadow:0 3px 8px rgba(110,100,170,.12);}
.settitle{font-size:17px; font-weight:800; color:var(--ink);}
.setcard{background:rgba(255,255,255,.62); backdrop-filter:blur(10px); border:1px solid rgba(255,255,255,.95);
   border-radius:20px; margin:0 14px 12px; padding:2px 0; box-shadow:0 4px 12px rgba(110,100,170,.08);}
.setrow{display:flex; align-items:center; justify-content:space-between; padding:13px 16px;
         font-size:13.5px; color:var(--ink);}
.setrow + .setrow{border-top:1px solid rgba(139,124,246,.14);}
.setrow .l{font-weight:600;}
.setrow .right{display:inline-flex; align-items:center; gap:5px; color:var(--ink2); font-size:12.5px;}
.switch{position:relative; width:46px; height:27px; border-radius:999px; background:#DDD7EF; flex:none;}
.switch i{position:absolute; top:3px; left:3px; width:21px; height:21px; border-radius:50%; background:#fff;
   box-shadow:0 2px 5px rgba(0,0,0,.22);}
.switch.on{background:linear-gradient(135deg,#8B7CF6,#5FB4FF);}
.switch.on i{left:22px;}
.setfoot{margin:6px 22px 0; font-size:10.5px; color:var(--ink2); line-height:1.7; text-align:center;}
/* 子页面（分类管理 / 账户管理 / 主题） */
.subsec{text-align:center; font-size:15px; font-weight:800; letter-spacing:1px; margin:48px auto 8px; color:var(--ink);}
.subsec::before{content:""; display:block; width:56px; height:3px; border-radius:3px; margin:0 auto 16px;
   background:linear-gradient(90deg,#8B7CF6,#5FB4FF);}
.addbtn{width:34px; height:34px; border-radius:50%; display:flex; align-items:center; justify-content:center;
   color:#8B7CF6; background:rgba(255,255,255,.74); backdrop-filter:blur(10px);
   border:1px solid rgba(255,255,255,.95); box-shadow:0 3px 8px rgba(110,100,170,.12);}
.seg2wrap{padding:0 14px 12px;}
.seg2{display:inline-flex; background:rgba(255,255,255,.7); border-radius:999px; padding:3px;
      backdrop-filter:blur(8px); border:1px solid rgba(255,255,255,.9);}
.seg2 span{padding:7px 24px; border-radius:999px; font-size:12.5px; color:var(--ink2);}
.seg2 span.on{background:linear-gradient(135deg,#8B7CF6,#5FB4FF); color:#fff; font-weight:800;}
.mrow .grip{color:#C9C2DE; display:inline-flex;}
.mrow .cc{width:20px; height:20px; border-radius:8px; flex:none;
          box-shadow:inset 0 0 0 1px rgba(255,255,255,.85);}
.mrow .nm{font-weight:700; flex:1; text-align:left;}
.tagp{font-size:10px; padding:2px 9px; border-radius:999px; flex:none;}
.tagp.home{background:#EDE7FB; color:#6C5CE7; font-weight:800;}
.tagp.extra{background:#EFEFF3; color:#9A9DB3;}
.bal{font-size:13.5px; font-weight:800; font-variant-numeric:tabular-nums; color:var(--ink);}
.dashrow{display:flex; align-items:center; justify-content:center; gap:6px; padding:13px 0; color:#8B7CF6;
         font-weight:800; font-size:13px; border-top:1.5px dashed rgba(139,124,246,.4);}
.themeopt{display:flex; align-items:center; gap:12px; padding:14px 16px;}
.themeopt + .themeopt{border-top:1px solid rgba(139,124,246,.14);}
.opt-ic{width:40px; height:40px; border-radius:14px; background:rgba(255,255,255,.75); flex:none;
        display:flex; align-items:center; justify-content:center; color:#8B7CF6;
        box-shadow:0 3px 8px rgba(110,100,170,.10);}
.opt-txt{flex:1; display:flex; flex-direction:column; gap:3px;}
.opt-txt .tt{font-size:14px; font-weight:800; color:var(--ink);}
.opt-txt .td{font-size:10.5px; color:var(--ink2);}
.radio{width:21px; height:21px; border-radius:50%; border:2px solid #CFC7E8; flex:none;}
.themeopt.sel .radio{border-color:#8B7CF6; position:relative;}
.themeopt.sel .radio::after{content:""; position:absolute; inset:3px; border-radius:50%;
   background:linear-gradient(135deg,#8B7CF6,#5FB4FF);}
'''
)

F = dict(
    file='style-f-editorial.html',
    name='F · 印刻（瑞士编辑排版）',
    sub='纸白底 + 炭黑大字 + 单一朱红强调；分类以"01–08"编号代替图案，导航为纯文字下划线。靠字号层级与细线网格排版，像一本严谨的财务刊物。',
    palette=[('#FAFAF7','纸白'),('#16181D','炭黑'),('#E5484D','朱红唯一强调'),('#1F7A5C','收入墨绿')],
    traits=['分类编号 01–08','文字下划线导航','大字编辑排版','单一朱红强调','细线网格大留白'],
    nav='text',
    g=[('#FFFFFF','#16181D','01'),('#FFFFFF','#16181D','02'),('#FFFFFF','#16181D','03'),('#FFFFFF','#16181D','04'),
       ('#FFFFFF','#16181D','05'),('#FFFFFF','#16181D','06'),('#FFFFFF','#16181D','07'),('#FFFFFF','#16181D','08')],
    rows=[('#FFFFFF','#16181D','', '午餐', '餐饮 · 现金 · 和同事', '−23.50', 'minus'),
          ('#FFFFFF','#16181D','', '咖啡', '餐饮 · 现金', '−18.00', 'minus'),
          ('#FFFFFF','#16181D','', '地铁', '交通 · 微信零钱', '−6.00', 'minus'),
          ('#FFFFFF','#16181D','', '红包', '收入 · 微信零钱', '＋200.00', 'plus'),
          ('#FFFFFF','#16181D','', '房租', '居住 · 储蓄卡 · 11月房租', '−1,200.00', 'minus'),
          ('#FFFFFF','#16181D','', '超市', '购物 · 支付宝', '−56.80', 'minus')],
    css=r'''
:root{
  --page:#EFEDE6; --frame:#B9B4A8; --screen:#FFFFFF;
  --ink:#16181D; --ink2:#8A8F98; --line:#E9E6DE;
  --accent:#E5484D; --accent2:#E5484D; --accent-soft:#FAF6EF;
  --income:#1F7A5C; --expense:#16181D;
  --card:#FFFFFF; --card-border:#E4E0D5;
  --c-food:#16181D; --c-rent:#E5484D; --c-shop:#7A7F88; --c-travel:#B0A99D; --c-income:#1F7A5C;
}
h1{letter-spacing:3px;}
.amount{font-weight:200; font-size:54px; letter-spacing:2px;}
.amount small{font-weight:400; font-size:20px;}
.seg{background:transparent; border-bottom:1px solid var(--line); border-radius:0; margin:8px 16px 0; padding:0;}
.seg span{background:transparent; color:#8A8F98; border-bottom:2px solid transparent; border-radius:0;
          padding:8px 18px; font-size:13.5px; letter-spacing:4px;}
.seg span.on{background:transparent; color:#16181D; font-weight:800; border-bottom:2px solid #E5484D; margin-bottom:-1px;}
.catgrid{grid-template-columns:1fr 1fr; gap:8px;}
.cat{border:1px solid #E8E4DA; border-radius:3px; flex-direction:row; justify-content:flex-start; gap:10px;
     padding:11px 12px; background:#FFFFFF; color:#16181D; font-size:12.5px; letter-spacing:1px; font-weight:600;}
.cat .e{color:#C2BBA9; font-weight:700; font-size:10.5px; letter-spacing:1px;}
.cat.hot{border-color:#16181D;}
.cat.hot .e{color:#E5484D;}
.save{border-radius:2px; font-weight:800; letter-spacing:8px; font-size:14px;}
.chip{border-radius:2px;}
.chip.star{background:#FBF5EE; border-color:#E8D9C6;}
.tile{display:none;}
.row{border-bottom:1px dashed #ECE8DE; padding-left:2px;}
.row .txt .n{font-size:14px; font-weight:600;}
.row .amt{font-weight:800;}
.agg .box{border-radius:2px;}
.card{border-radius:0; border-left:3px solid #16181D;}
.card .v{font-weight:900; font-size:19px;}
.card .t{letter-spacing:2px;}
.pill{border-radius:2px; letter-spacing:2px; font-weight:700;}
.track{border-radius:1px;}
.month{font-size:12px; letter-spacing:3px;}
.bars .v{background:#16181D;}
.bars .b.lo .v{background:#D9D4C8;}
.bars .b.hi .v{background:#E5484D;}
.nav{border-top:1px solid #16181D;}
.nav .ic{display:none;}
.nav .item{padding-top:12px; font-size:12px; letter-spacing:5px; color:#9B968B; font-weight:600;}
.nav .item.on{color:#16181D; font-weight:800; box-shadow:inset 0 -2.5px 0 0 #E5484D;}
'''
)

THEMES = [D, E, F]

def main():
    here = os.path.dirname(os.path.abspath(__file__))
    for t in THEMES:
        out = os.path.join(here, t['file'])
        with open(out, 'w', encoding='utf-8') as f:
            f.write(build(t))
        print('written', t['file'], os.path.getsize(out), 'bytes')

if __name__ == '__main__':
    main()
