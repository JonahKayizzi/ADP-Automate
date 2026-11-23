type Props = { title: string }

export function PlaceholderPage({ title }: Props) {
  return (
    <section>
      <h1 className="font-display text-3xl font-semibold text-ink">{title}</h1>
      <p className="mt-2 max-w-2xl text-muted">
        Module scaffold only — domain screens arrive in later phases.
      </p>
    </section>
  )
}
