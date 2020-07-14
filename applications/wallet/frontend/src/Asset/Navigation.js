import PropTypes from 'prop-types'
import Link from 'next/link'

import { constants, colors, spacing } from '../Styles'

import Button, { VARIANTS } from '../Button'

import BackSvg from '../Icons/back.svg'

const ICON_SIZE = 20

const AssetNavigation = ({ projectId, assetId, query }) => {
  const idString = `?id=${assetId}`
  const queryString = query ? `&query=${query}` : ''

  return (
    <div
      css={{
        paddingLeft: spacing.base,
        paddingRight: spacing.base,
        backgroundColor: colors.structure.lead,
        color: colors.structure.steel,
        boxShadow: constants.boxShadows.navBar,
        marginBottom: spacing.hairline,
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        width: '100%',
      }}
    >
      <Link
        href={`/[projectId]/visualizer${idString}${queryString}`}
        as={`/${projectId}/visualizer${idString}${queryString}`}
        passHref
      >
        <Button
          variant={VARIANTS.NEUTRAL}
          css={{
            padding: spacing.base,
            color: colors.structure.steel,
            borderRadius: constants.borderRadius.small,
            ':hover': {
              color: colors.structure.white,
            },
          }}
        >
          <BackSvg height={ICON_SIZE} />
        </Button>
      </Link>
    </div>
  )
}

AssetNavigation.propTypes = {
  projectId: PropTypes.string.isRequired,
  assetId: PropTypes.string.isRequired,
  query: PropTypes.string.isRequired,
}

export default AssetNavigation
