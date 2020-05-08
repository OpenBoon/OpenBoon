import PropTypes from 'prop-types'

import filterShape from '../Filter/shape'

import { colors, constants, spacing } from '../Styles'

import Button, { VARIANTS } from '../Button'

import { dispatch, ACTIONS } from './helpers'
import FacetSvg from '../Icons/facet.svg'
import RangeSvg from '../Icons/range.svg'
import ExistsSvg from '../Icons/exists.svg'
import MissingSvg from '../Icons/missing.svg'
import HiddenSvg from '../Icons/hidden.svg'
import CrossSvg from '../Icons/cross.svg'

const SVG_SIZE = 20
const OPTION_CONTAINER = 32

const noop = () => {}

const FiltersTitle = ({
  projectId,
  assetId,
  filter: { attribute, type, values },
  filters,
  filterIndex,
}) => {
  return (
    <div
      css={{
        width: '100%',
        display: 'flex',
        alignItems: 'center',
      }}
    >
      {(type === 'facet' || type === 'labelConfidence') && (
        <FacetSvg width={SVG_SIZE} color={colors.key.one} />
      )}
      {type === 'range' && <RangeSvg width={SVG_SIZE} color={colors.key.one} />}
      {type === 'exists' &&
        (values && values.exists ? (
          <ExistsSvg width={SVG_SIZE} color={colors.key.one} />
        ) : (
          <MissingSvg width={SVG_SIZE} color={colors.key.one} />
        ))}

      <div
        css={{
          flex: 1,
          minWidth: 0,
          display: 'flex',
          alignItems: 'center',
        }}
      >
        <span
          css={{
            fontFamily: 'Roboto Mono',
            paddingLeft: spacing.base,
            overflow: 'hidden',
            whiteSpace: 'nowrap',
            textOverflow: 'ellipsis',
          }}
        >
          {attribute}
        </span>
      </div>
      <Button
        variant={VARIANTS.ICON}
        css={{
          width: OPTION_CONTAINER,
          height: OPTION_CONTAINER,
          display: 'flex',
          justifyContent: 'center',
          alignItems: 'center',
          borderRadius: constants.borderRadius.small,
          ':hover': {
            backgroundColor: colors.structure.smoke,
            svg: { color: colors.structure.white },
          },
        }}
        onClick={noop}
      >
        <HiddenSvg
          width={SVG_SIZE}
          color={colors.structure.steel}
          css={{
            display: 'none',
          }}
        />
      </Button>
      <Button
        variant={VARIANTS.ICON}
        css={{
          width: OPTION_CONTAINER,
          height: OPTION_CONTAINER,
          display: 'flex',
          justifyContent: 'center',
          alignItems: 'center',
          borderRadius: constants.borderRadius.small,
          ':hover': {
            backgroundColor: colors.structure.smoke,
            svg: { color: colors.structure.white },
          },
        }}
        onClick={() =>
          dispatch({
            action: ACTIONS.DELETE_FILTER,
            payload: {
              projectId,
              assetId,
              filters,
              filterIndex,
            },
          })
        }
      >
        <CrossSvg
          width={SVG_SIZE}
          color={colors.structure.steel}
          css={{
            display: 'none',
          }}
        />
      </Button>
    </div>
  )
}

FiltersTitle.propTypes = {
  projectId: PropTypes.string.isRequired,
  assetId: PropTypes.string.isRequired,
  filters: PropTypes.arrayOf(PropTypes.shape(filterShape)).isRequired,
  filter: PropTypes.shape(filterShape).isRequired,
  filterIndex: PropTypes.number.isRequired,
}

export default FiltersTitle
