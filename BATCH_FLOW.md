# Exact batch flow

START
→ load enabled account list in saved order
→ account 1
→ launch configured package
→ wait for UI
→ scan active window
→ require Marketplace indicator
→ locate sender/message markers
→ reserve first-reply slot
→ render template
→ Dry Run OR send
→ mark REPLIED only after successful UI click
→ continue scanning until idle timeout
→ mark account DONE
→ next account
→ after final account mark COMPLETE and stop

If package is missing: skip account.
If sender is ambiguous: do not reply.
If message is non-Marketplace: ignore.
If input field is missing: do not reply.
If Send/Kirim is missing: do not reply.
If send fails: release reservation and continue.
