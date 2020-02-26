import { constants, colors, spacing } from '../Styles'

const AssetsInfoBar = () => {
  // Temporary values
  const displayCount = 100
  const totalCount = 2400

  return (
    <div css={{ width: '100%' }}>
      <div
        css={{
          height: constants.navbar.height,
          display: 'flex',
          justifyContent: 'flex-end',
          alignItems: 'center',
          backgroundColor: colors.structure.mattGrey,
          boxShadow: constants.boxShadows.infoBar,
          paddingLeft: spacing.normal,
          paddingRight: spacing.normal,
          fontFamily: 'Roboto Condensed',
        }}>
        <div css={{ color: colors.structure.steel }}>
          Sort: Import Date &nbsp;| &nbsp;
        </div>
        <div css={{ color: colors.structure.zinc }}>
          {displayCount} of {totalCount} Results
        </div>
      </div>
    </div>
  )
}

export default AssetsInfoBar
