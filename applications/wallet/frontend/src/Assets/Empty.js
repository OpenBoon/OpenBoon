import { colors, spacing, typography } from '../Styles'

import NoAssetsSvg from '../Icons/noAssets.svg'

const AssetsEmpty = () => {
  return (
    <div
      css={{
        height: '100%',
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'center',
        alignItems: 'center',
        padding: spacing.normal,
      }}
    >
      <NoAssetsSvg width={168} color={colors.structure.steel} />
      <h2
        css={{
          paddingTop: spacing.normal,
          fontSize: typography.size.giant,
          lineHeight: typography.height.giant,
        }}
      >
        There are currently no assets to show.
      </h2>
      <h3
        css={{
          fontSize: typography.size.large,
          lineHeight: typography.height.large,
          color: colors.structure.zinc,
        }}
      >
        Either all have been filtered out or there aren’t any in the system yet.
      </h3>
    </div>
  )
}

export default AssetsEmpty
