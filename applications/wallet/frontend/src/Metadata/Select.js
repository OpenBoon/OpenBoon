import { colors, spacing, typography } from '../Styles'

const MetadataSelect = () => {
  return (
    <>
      <div css={{ padding: spacing.normal }}>
        <div
          css={{
            color: colors.key.one,
            fontStyle: typography.style.italic,
            whiteSpace: 'nowrap',
          }}
        >
          Select an asset to view its metadata
        </div>
      </div>
      <div
        css={{
          height: '100%',
          overflow: 'auto',
          backgroundColor: colors.structure.mattGrey,
          padding: spacing.normal,
        }}
      />
    </>
  )
}

export default MetadataSelect
