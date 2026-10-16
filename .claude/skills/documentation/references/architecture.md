# High-level service architecture

The architecture overview describes the service's purposes, external relationships and main flows. Reusable coding
rules and internal design notes are separate from this high-level view.

## Scope

State the service's main business purposes briefly. Show the service as one unit in a C4-style context/container view.
Show relevant external consumers, upstream/downstream services, databases, brokers, or other systems only when verified.
Label relationship direction and purpose. Include protocol, event, or technology names only when known and useful. Keep
data ownership and significant operational constraints concise and at service level.

Do not show classes, packages, controllers, repositories, internal layers, or their interactions. Do not turn the
overview into a feature list, endpoint catalog, deployment inventory, or record of small implementation changes.

## Diagrams

Maintain a service/external-system overview and a concise flow diagram for each distinct main purpose. Combine flows
when clearer. Use Mermaid flowcharts or sequence diagrams in Markdown; follow an established equivalent if present.

Flow diagrams show the main trigger, service action, important external interactions, and business outcome. Keep minor
branches and implementation steps out. Include a failure path only when it explains a significant service
responsibility. Do not invent a datastore, RabbitMQ integration, actor, or flow merely to fill a diagram. If the
scaffold has no defined purpose or implemented interactions, state that clearly and leave diagrams absent.

## Evidence

Trace relationships and flows through actual code/configuration and agreed requirements. Distinguish current behavior
from approved future plans and unresolved assumptions. Update affected arrows, labels, and purpose summaries together.
Keep diagrams and prose consistent. Check Mermaid syntax when a suitable existing renderer is available; otherwise
report that rendering was not checked.
