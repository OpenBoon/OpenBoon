import PropTypes from 'prop-types'

import { colors, spacing, typography, constants } from '../Styles'

import DoubleChevronSvg from '../Icons/doubleChevron.svg'

import Button, { VARIANTS } from '../Button'
import Resizeable from '../Resizeable'
import BetaBadge from '../BetaBadge'

const MIN_WIDTH = 400

const PanelContent = ({
  openToThe,
  panel: { title, content, isBeta },
  setOpenPanel,
}) => {
  return (
    <Resizeable
      minSize={MIN_WIDTH}
      storageName={`${openToThe}OpeningPanelWidth`}
      openToThe={openToThe}
      onMouseUp={({ size }) => {
        if (size < MIN_WIDTH) setOpenPanel({ value: '' })
      }}
    >
      <div
        css={{
          display: 'flex',
          flexDirection: 'column',
          height: '100%',
          backgroundColor: colors.structure.lead,
          [openToThe === 'left'
            ? 'marginRight'
            : 'marginLeft']: spacing.hairline,
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
            {title}
            {isBeta && <BetaBadge />}
          </h2>
          <Button
            aria-label="Close Panel"
            variant={VARIANTS.ICON}
            onClick={() => setOpenPanel({ value: '' })}
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
            // hack to make content scroll without hiding overflow (overflow needed for Toggltip visibility)
            height: '0%',
          }}
        >
          {content}
        </div>
      </div>
    </Resizeable>
  )
}

PanelContent.propTypes = {
  openToThe: PropTypes.oneOf(['left', 'right']).isRequired,
  panel: PropTypes.shape({
    title: PropTypes.string.isRequired,
    icon: PropTypes.node.isRequired,
    content: PropTypes.node.isRequired,
    isBeta: PropTypes.bool,
  }).isRequired,
  setOpenPanel: PropTypes.func.isRequired,
}

export default PanelContent
