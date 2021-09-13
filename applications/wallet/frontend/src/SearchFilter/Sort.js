import PropTypes from 'prop-types'

import { spacing, constants, zIndex } from '../Styles'

import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'
import { dispatch, ACTIONS } from '../Filters/helpers'
import Listbox from '../Listbox'

import BackSvg from '../Icons/back.svg'
import CrossSvg from '../Icons/cross.svg'

import { formatFields } from './helpers'

const SearchFilterSort = ({
  pathname,
  projectId,
  assetId,
  filters,
  fields,
}) => {
  const filteredFields = formatFields({ fields })

  const filterIndex = filters.findIndex(({ type }) => type === 'simpleSort')

  const sortFilter = filters.find(({ type }) => type === 'simpleSort')

  const splitAttribute = !sortFilter ? [] : sortFilter?.attribute.split('.')
  const shortenedAttribute = splitAttribute[splitAttribute.length - 1]

  return (
    <div
      css={{
        display: 'flex',
        label: {
          width: '100%',
          'div:first-of-type': { display: 'none' },
          '[data-reach-listbox-input]': {
            borderTopLeftRadius: 0,
            borderBottomLeftRadius: 0,
          },
          'span[role=button]': { padding: spacing.moderate },
        },
      }}
    >
      <Button
        aria-label="Change sort direction"
        variant={BUTTON_VARIANTS.SECONDARY}
        onClick={() => {
          dispatch({
            type: ACTIONS.UPDATE_FILTER,
            payload: {
              pathname,
              projectId,
              assetId,
              filters,
              updatedFilter: {
                ...sortFilter,
                values: {
                  order: sortFilter?.values?.order === 'asc' ? 'desc' : 'asc',
                },
              },
              filterIndex,
            },
          })
        }}
        isDisabled={!sortFilter}
        style={{
          padding: spacing.moderate,
          marginRight: spacing.hairline,
          borderTopRightRadius: 0,
          borderBottomRightRadius: 0,
          zIndex: zIndex.layout.interactive,
        }}
      >
        <BackSvg
          height={constants.icons.regular}
          css={{
            transform:
              sortFilter?.values?.order === 'asc'
                ? 'rotate(90deg)'
                : 'rotate(-90deg)',
          }}
        />
      </Button>

      <Listbox
        key={shortenedAttribute}
        label="Sort by"
        inputLabel="Filter sort options"
        value={sortFilter?.attribute || ''}
        placeholder={shortenedAttribute || 'Select sort option'}
        options={filteredFields}
        onChange={({ value }) => {
          if (filterIndex === -1) {
            dispatch({
              type: ACTIONS.ADD_FILTERS,
              payload: {
                pathname,
                projectId,
                assetId,
                filters,
                newFilters: [
                  {
                    type: 'simpleSort',
                    attribute: value,
                    values: { order: sortFilter?.values?.order || 'desc' },
                  },
                ],
              },
            })
          } else {
            dispatch({
              type: ACTIONS.UPDATE_FILTER,
              payload: {
                pathname,
                projectId,
                assetId,
                filters,
                updatedFilter: {
                  type: 'simpleSort',
                  attribute: value,
                  values: { order: sortFilter?.values?.order },
                },
                filterIndex,
              },
            })
          }
        }}
      />

      <Button
        aria-label="Clear sort"
        variant={BUTTON_VARIANTS.SECONDARY}
        onClick={() => {
          dispatch({
            type: ACTIONS.DELETE_FILTER,
            payload: {
              pathname,
              projectId,
              assetId,
              filters,
              filterIndex,
            },
          })
        }}
        isDisabled={!sortFilter}
        style={{
          padding: spacing.moderate,
          marginLeft: spacing.small,
        }}
      >
        <CrossSvg height={constants.icons.regular} />
      </Button>
    </div>
  )
}

SearchFilterSort.propTypes = {
  pathname: PropTypes.string.isRequired,
  projectId: PropTypes.string.isRequired,
  assetId: PropTypes.string.isRequired,
  filters: PropTypes.arrayOf(PropTypes.shape({}).isRequired).isRequired,
  fields: PropTypes.shape({}).isRequired,
}

export default SearchFilterSort
