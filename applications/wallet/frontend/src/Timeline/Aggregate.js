import PropTypes from 'prop-types'

import { colors, constants } from '../Styles'

import Menu from '../Menu'
import MenuButton from '../Menu/Button'
import Checkbox, { VARIANTS as CHECKBOX_VARIANTS } from '../Checkbox'

export const noop = () => {}

const TimelineAggregate = ({ detections, timelineHeight }) => {
  return (
    <div
      css={{
        display: 'flex',
        marginLeft: -constants.timeline.modulesWidth,
        borderTop: constants.borders.regular.smoke,
        height: constants.timeline.rulerRowHeight,
      }}
    >
      <div css={{ width: constants.timeline.modulesWidth }}>
        <Menu
          open="bottom-center"
          button={({ onBlur, onClick, isMenuOpen }) => (
            <MenuButton
              onBlur={onBlur}
              onClick={onClick}
              legend={`Detections (${detections.length})`}
              style={{
                '&,&:hover,&:visited': {
                  backgroundColor: isMenuOpen
                    ? colors.structure.steel
                    : colors.structure.smoke,
                },
                '&:hover': {
                  backgroundColor: colors.structure.steel,
                },
                '&[aria-disabled=true]': {
                  backgroundColor: colors.structure.smoke,
                },
                marginBottom: 0,
                borderRadius: 0,
                height: '100%',
              }}
            />
          )}
        >
          {({ onBlur }) => (
            <div
              // eslint-disable-next-line
              tabIndex="1"
              css={{
                backgroundColor: colors.structure.iron,
                maxHeight:
                  timelineHeight - constants.timeline.rulerRowHeight * 3,
                overflowY: 'auto',
              }}
            >
              <div css={{ borderBottom: constants.borders.medium.steel }}>
                <Checkbox
                  variant={CHECKBOX_VARIANTS.MENU}
                  option={{
                    value: 'all',
                    label: 'All',
                    initialValue: true,
                    isDisabled: false,
                  }}
                  onBlur={onBlur}
                  onClick={noop}
                />
              </div>

              {detections.map(({ name, predictions }) => (
                <Checkbox
                  key={name}
                  variant={CHECKBOX_VARIANTS.MENU}
                  option={{
                    value: name,
                    label: name,
                    legend: `(${predictions.length})`,
                    initialValue: true,
                    isDisabled: false,
                  }}
                  onBlur={onBlur}
                  onClick={noop}
                />
              ))}
            </div>
          )}
        </Menu>
      </div>
    </div>
  )
}

TimelineAggregate.propTypes = {
  detections: PropTypes.arrayOf(PropTypes.shape({})).isRequired,
  timelineHeight: PropTypes.number.isRequired,
}

export default TimelineAggregate
