#!/usr/bin/env bash
# Scores a labelled classification sample against what the rules answered for it. A mistake is a
# row whose state differs from the label; a reason that differs is not a mistake, since the
# reasons exist to tell the body pass which question to ask.
#
#     scripts/score-classification-sample.sh <sample-dir> <labels.tsv>
#
# <sample-dir> is what draw-classification-sample.sh wrote. <labels.tsv> is the label fixture:
# id, cleaned title, label and reason, with # comment lines. Prints the numbers as Markdown, and
# writes every row whose state differs to <sample-dir>/disagreements.tsv as id, title, label,
# label reason, predicted state and predicted reason.

source "$(dirname "$0")/lib.sh"

dir="${1:?usage: score-classification-sample.sh <sample-dir> <labels.tsv>}"
labels="${2:?usage: score-classification-sample.sh <sample-dir> <labels.tsv>}"

rm -f "$dir/disagreements.tsv"
awk -F'\t' -v dir="$dir" '
	FILENAME ~ /corpus\.tsv$/ {
		if ($1 == "eligible") eligible = $3
		else { corpus += $3; if ($2 == "unruled") unruled = $3 }
		next
	}
	FILENAME ~ /predictions\.tsv$/ { state[$1] = $2; why[$1] = $3; next }
	/^#/ || NF == 0 { next }
	!($1 in state) { missing++; next }
	{
		p = state[$1]; l = $3
		n[p]++
		if (l != p) {
			wrong[p]++
			print $1 "\t" $2 "\t" l "\t" $4 "\t" p "\t" why[$1] > (dir "/disagreements.tsv")
		}
		if (p == "OUT" && l == "IN") { outIn++; outInTitles = outInTitles "\n  - `" $2 "`" }
		if (l == "UNKNOWN") { pile[$4]++; if (p != "UNKNOWN") decided[p]++ }
		if (p == "UNKNOWN") { reasons[why[$1]] = 1; cell[why[$1], l]++; rows[why[$1]]++ }
	}
	function rate(s) { return sprintf("**%.2f%%** (%d of %d)", n[s] ? 100 * wrong[s] / n[s] : 0, wrong[s], n[s]) }
	END {
		if (missing) print "WARNING: " missing " labelled ids are not in predictions.tsv\n"
		print "| Gated number | This round |"
		print "| --- | ---: |"
		print "| `OUT` stratum error — labelled `IN` or `UNKNOWN` | " rate("OUT") " |"
		print "| `IN` stratum error — labelled `OUT` or `UNKNOWN` | " rate("IN") " |"
		print "| `UNKNOWN` stratum error — labelled `IN` or `OUT` | " rate("UNKNOWN") " |"
		printf "| `unruled` — of the whole corpus | **%.2f%%** (%d of %d) |\n", 100 * unruled / corpus, unruled, corpus
		print ""
		print "- **`OUT`-stratum rows labelled `IN`**: **" outIn + 0 "** of " n["OUT"] "." outInTitles
		print "- **Rows labelled `UNKNOWN` that the rules decided**: **" decided["IN"] + decided["OUT"] "** — " decided["IN"] + 0 " `IN`, " decided["OUT"] + 0 " `OUT`."
		sep = ""; line = ""
		for (r in pile) { line = line sep r " " pile[r]; sep = ", " }
		print "- **The labeller'\''s unknown pile**: " line "."
		print "- **Eligible rows** the sample was drawn from: " eligible "."
		print ""
		print "| Rules'\'' reason | Labelled `IN` | Labelled `OUT` | Labelled `UNKNOWN` |"
		print "| --- | ---: | ---: | ---: |"
		for (r in reasons) print "| `" r "` (" rows[r] ") | " cell[r, "IN"] + 0 " | " cell[r, "OUT"] + 0 " | " cell[r, "UNKNOWN"] + 0 " |"
	}
' "$dir/corpus.tsv" "$dir/predictions.tsv" "$labels"
