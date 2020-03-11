import PropTypes from 'prop-types'

import RefreshSvg from '../Icons/refresh.svg'

import { colors, spacing } from '../Styles'

import Button, { VARIANTS } from '../Button'

const REFRESH_HEIGHT = 32

const Refresh = ({ onClick }) => {
  return (
    <Button
      variant={VARIANTS.PRIMARY}
      style={{
        height: REFRESH_HEIGHT,
      }}
      onClick={onClick}>
      <div
        css={{
          display: 'flex',
          alignItems: 'center',
        }}>
        <RefreshSvg width={20} color={colors.structure.white} />
        <div css={{ paddingLeft: spacing.small }}>Refresh Jobs</div>
      </div>
    </Button>
  )
}

Refresh.propTypes = {
  onClick: PropTypes.func.isRequired,
}

export default Refresh
