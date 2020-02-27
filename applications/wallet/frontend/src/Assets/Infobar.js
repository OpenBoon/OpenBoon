import { constants, colors, spacing } from '../Styles'

const AssetsInfoBar = () => {
  // Temporary values
  const displayCount = 100
  const totalCount = 2400

  return (
    <div
      css={{
        height: constants.navbar.height,
        display: 'flex',
        alignItems: 'center',
        backgroundColor: colors.structure.mattGrey,
        boxShadow: constants.boxShadows.infoBar,
        fontFamily: 'Roboto Condensed',
      }}>
      <div css={{ flex: 1 }} />
      <div css={{ color: colors.structure.steel, padding: spacing.base }}>
        Sort: Import Date
      </div>
      <div css={{ color: colors.structure.steel }}>|</div>
      <div
        css={{
          color: colors.structure.zinc,
          padding: spacing.base,
          paddingRight: spacing.normal,
        }}>
        {displayCount} of {totalCount} Results
      </div>
    </div>
  )
}

export default AssetsInfoBar
