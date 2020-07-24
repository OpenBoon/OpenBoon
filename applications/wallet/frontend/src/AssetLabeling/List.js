import { colors, spacing, typography } from '../Styles'

const AssetLabelingList = () => {
  return (
    <div css={{ padding: spacing.normal }}>
      <div css={{ display: 'flex', color: colors.structure.steel }}>
        <div
          css={{
            fontFamily: typography.family.condensed,
            padding: spacing.moderate,
            paddingLeft: spacing.normal,
            paddingRight: spacing.base,
            flex: 1,
          }}
        >
          <span css={{ textTransform: 'uppercase' }}>Model</span>
        </div>
        <div
          css={{
            flex: 4,
            fontFamily: typography.family.mono,
            fontSize: typography.size.small,
            lineHeight: typography.height.small,
            padding: spacing.moderate,
            wordBreak: 'break-all',
          }}
        >
          <span css={{ textTransform: 'uppercase' }}>Label</span>
        </div>
      </div>
    </div>
  )
}

export default AssetLabelingList
