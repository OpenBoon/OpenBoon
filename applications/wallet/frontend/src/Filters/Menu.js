import { useState } from 'react'
import PropTypes from 'prop-types'
import useSWR from 'swr'

import filterShape from '../Filter/shape'

import { spacing, constants, zIndex } from '../Styles'

import PlusSvg from '../Icons/plus.svg'
import CrossSvg from '../Icons/crossSmall.svg'

import Accordion, { VARIANTS as ACCORDION_VARIANTS } from '../Accordion'
import Button, { VARIANTS } from '../Button'

import { formatDisplayName } from '../Metadata/helpers'

import { dispatch, ACTIONS } from './helpers'

import FiltersMenuSection from './MenuSection'

const FiltersMenu = ({ projectId, assetId, filters, setIsMenuOpen }) => {
  const { data: fields } = useSWR(
    `/api/v1/projects/${projectId}/searches/fields/`,
  )

  const [newFilters, setNewFilters] = useState({})

  const onClick = ({ type, attribute }) => (value) => {
    if (value) {
      const values = type === 'exists' ? { exists: true } : {}

      setNewFilters((nF) => ({
        ...nF,
        [attribute]: { type, attribute, values },
      }))
    } else {
      setNewFilters((nF) => {
        const { [attribute]: filterToRemove, ...rest } = nF
        return rest
      })
    }
  }

  return (
    <>
      <div
        css={{
          height: 1,
          boxShadow: constants.boxShadows.standalone,
          zIndex: zIndex.layout.interactive,
          marginTop: -1,
        }}
      />
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
                title={formatDisplayName({ name: key })}
                cacheKey={`Filters.${key}`}
                isInitiallyOpen={false}
                isResizeable={false}
              >
                <div
                  css={{
                    padding: spacing.normal,
                    paddingTop: spacing.base,
                    paddingBottom: spacing.base,
                  }}
                >
                  {Object.entries(value)
                    .filter((entry) => entry[1][0] !== 'similarity')
                    .map(([subKey, subValue]) => {
                      return (
                        <FiltersMenuSection
                          key={subKey}
                          path={key}
                          attribute={subKey}
                          value={subValue}
                          filters={filters}
                          onClick={onClick}
                        />
                      )
                    })}
                </div>
              </Accordion>
            ),
          )}
        </div>

        <div css={{ padding: spacing.base, display: 'flex' }}>
          <Button
            aria-label="Cancel"
            variant={VARIANTS.SECONDARY}
            onClick={() => setIsMenuOpen(false)}
            style={{
              flex: 1,
              display: 'flex',
              flexFlow: 'nowrap',
              alignItems: 'end',
              svg: { marginRight: spacing.base },
            }}
          >
            <CrossSvg width={18} />
            Cancel
          </Button>

          <div css={{ width: spacing.base, minWidth: spacing.base }} />

          <Button
            aria-label="Add Selected Filters"
            variant={VARIANTS.PRIMARY}
            onClick={() => {
              dispatch({
                action: ACTIONS.ADD_FILTERS,
                payload: {
                  projectId,
                  assetId,
                  filters,
                  newFilters: Object.values(newFilters),
                },
              })

              setIsMenuOpen(false)
            }}
            style={{
              flex: 1,
              display: 'flex',
              flexFlow: 'nowrap',
              alignItems: 'end',
              svg: { marginRight: spacing.base },
            }}
            isDisabled={Object.keys(newFilters).length === 0}
          >
            <PlusSvg width={16} />
            Add Selected Filters
          </Button>
        </div>
      </div>
    </>
  )
}

FiltersMenu.propTypes = {
  projectId: PropTypes.string.isRequired,
  assetId: PropTypes.string.isRequired,
  filters: PropTypes.arrayOf(PropTypes.shape(filterShape)).isRequired,
  setIsMenuOpen: PropTypes.func.isRequired,
}

export default FiltersMenu
