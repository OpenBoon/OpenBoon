import PropTypes from 'prop-types'

import { colors, spacing, typography, constants } from '../Styles'

import DoubleChevronSvg from '../Icons/doubleChevron.svg'

import { ACTIONS } from '../Resizeable/reducer'

import Button, { VARIANTS } from '../Button'
import BetaBadge from '../BetaBadge'

const PanelContent = ({
  openToThe,
  panel: { title, content, isBeta },
  dispatch,
}) => {
  return (
    <div
      css={{
        display: 'flex',
        flexDirection: 'column',
        height: '100%',
        backgroundColor: colors.structure.lead,
        [openToThe === 'left' ? 'marginRight' : 'marginLeft']: spacing.hairline,
      }}
    >
      <div
        css={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          padding: spacing.base,
          paddingLeft: spacing.normal,
          borderBottom: constants.borders.regular.smoke,
        }}
      >
        <h2
          css={{
            whiteSpace: 'nowrap',
            textTransform: 'uppercase',
            fontWeight: typography.weight.medium,
            fontSize: typography.size.regular,
            lineHeight: typography.height.regular,
            display: 'flex',
            alignItems: 'center',
          }}
        >
          {isBeta && <BetaBadge isLeft />}
          {title}
        </h2>
        <Button
          aria-label="Close Panel"
          variant={VARIANTS.ICON}
          onClick={() => {
            dispatch({
              type: ACTIONS.CLOSE,
              payload: { openPanel: '' },
            })
          }}
          style={{
            padding: 0,
          }}
        >
          <DoubleChevronSvg
            height={constants.icons.regular}
            css={{
              transform: `rotate(${openToThe === 'left' ? -90 : 90}deg)`,
            }}
          />
        </Button>
      </div>
      <div
        css={{
          display: 'flex',
          flexDirection: 'column',
          flex: 1,
          // hack to make content scroll without hiding overflow (overflow needed for Toggletip visibility)
          height: '0%',
        }}
      >
        {content}
      </div>
    </div>
  )
}

PanelContent.propTypes = {
  openToThe: PropTypes.oneOf(['left', 'right']).isRequired,
  panel: PropTypes.shape({
    title: PropTypes.string.isRequired,
    content: PropTypes.node.isRequired,
    isBeta: PropTypes.bool,
  }).isRequired,
  dispatch: PropTypes.func.isRequired,
}

export default PanelContent
