import PropTypes from 'prop-types'

import { spacing, typography, constants } from '../Styles'

import Accordion, { VARIANTS as ACCORDION_VARIANTS } from '../Accordion'
import Button, { VARIANTS } from '../Button'

import FiltersMenuOption from './MenuOption'

const FiltersMenu = ({
  // projectId,
  // assetId,
  // filters,
  fields,
  setIsMenuOpen,
}) => {
  const closeMenu = () => setIsMenuOpen(false)
  return (
    <div
      css={{
        overflow: 'hidden',
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'space-between',
        flex: 1,
      }}
    >
      <div css={{ overflowX: 'auto' }}>
        {Object.entries(fields).map(([key, value]) =>
          Array.isArray(value) ? null : (
            <Accordion
              key={key}
              variant={ACCORDION_VARIANTS.PANEL}
              title={key}
              isInitiallyOpen={false}
            >
              <div
                css={{
                  padding: spacing.normal,
                  paddingTop: spacing.base,
                  paddingBottom: spacing.base,
                }}
              >
                {Object.entries(value).map(([subKey, subValue], index, arr) =>
                  Array.isArray(subValue) ? (
                    <FiltersMenuOption key={subKey} option={subKey} />
                  ) : (
                    <div
                      key={subKey}
                      css={{
                        marginLeft: -spacing.normal,
                        marginRight: -spacing.normal,
                        padding: spacing.moderate,
                        paddingTop: index === 0 ? 0 : spacing.base,
                        paddingBottom:
                          index === arr.length - 1 ? 0 : spacing.base,
                        borderTop:
                          index !== 0 && !Array.isArray(subValue)
                            ? constants.borders.largeDivider
                            : '',
                      }}
                    >
                      <h4
                        css={{
                          fontFamily: 'Roboto Mono',
                          fontWeight: typography.weight.regular,
                        }}
                      >
                        {subKey}
                      </h4>
                      {Object.entries(subValue).map(([subSubKey]) => (
                        <FiltersMenuOption key={subSubKey} option={subSubKey} />
                      ))}
                    </div>
                  ),
                )}
              </div>
            </Accordion>
          ),
        )}
      </div>
      <div css={{ padding: spacing.base, display: 'flex' }}>
        <Button
          variant={VARIANTS.SECONDARY}
          onClick={closeMenu}
          style={{ flex: 1 }}
        >
          x Cancel
        </Button>
        <div css={{ width: spacing.base }} />
        <Button
          variant={VARIANTS.PRIMARY}
          onClick={closeMenu}
          style={{ flex: 1 }}
        >
          + Add Selected Filters
        </Button>
      </div>
    </div>
  )
}

FiltersMenu.propTypes = {
  // projectId: PropTypes.string.isRequired,
  // assetId: PropTypes.string.isRequired,
  // filters: PropTypes.arrayOf(
  //   PropTypes.shape({
  //     type: PropTypes.oneOf(['search', 'facet', 'range', 'exists']).isRequired,
  //     value: PropTypes.oneOfType([PropTypes.string, PropTypes.object])
  //       .isRequired,
  //   }).isRequired,
  // ).isRequired,
  fields: PropTypes.objectOf(PropTypes.objectOf).isRequired,
  setIsMenuOpen: PropTypes.func.isRequired,
}

export default FiltersMenu
