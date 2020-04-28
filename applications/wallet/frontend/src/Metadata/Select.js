import { colors, spacing, typography } from '../Styles'

const MetadataSelect = () => {
  return (
    <div css={{ padding: spacing.normal }}>
      <div
        css={{
          color: colors.key.one,
          fontStyle: typography.style.italic,
        }}
      >
        Select an asset to view its metadata
      </div>
    </div>
  )
}

export default MetadataSelect
