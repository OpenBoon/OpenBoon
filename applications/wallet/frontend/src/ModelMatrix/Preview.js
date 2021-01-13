import { useRouter } from 'next/router'
import Link from 'next/link'
import PropTypes from 'prop-types'

import { colors, constants, spacing, typography } from '../Styles'

import { useLocalStorage } from '../LocalStorage/helpers'

import FilterSvg from '../Icons/filter.svg'

import { MIN_WIDTH as PANEL_MIN_WIDTH } from '../Panel'
import { ACTIONS, reducer as resizeableReducer } from '../Resizeable/reducer'

import { encode } from '../Filters/helpers'

import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'

const ModelMatrixPreview = ({ selectedCell, labels, moduleName }) => {
  const {
    query: { projectId, modelId },
  } = useRouter()

  const [, setRightOpeningPanel] = useLocalStorage({
    key: 'rightOpeningPanelSettings',
    reducer: resizeableReducer,
    initialState: {
      size: PANEL_MIN_WIDTH,
      originSize: 0,
      isOpen: false,
    },
  })

  if (!selectedCell.length) {
    return (
      <div
        css={{
          padding: spacing.normal,
          fontStyle: typography.style.italic,
          height: '100%',
          borderLeft: constants.borders.regular.coal,
        }}
      >
        Select a cell of the matrix to view the labeled assets.
      </div>
    )
  }

  const encodedFilter = encode({
    filters: [
      {
        type: 'label',
        attribute: `labels.${moduleName}`,
        modelId,
        values: {},
      },
    ],
  })

  return (
    <div
      css={{
        display: 'flex',
        flexDirection: 'column',
        height: '100%',
        borderLeft: constants.borders.regular.coal,
        overflow: 'auto',
      }}
    >
      <div
        css={{
          padding: spacing.normal,
          fontSize: typography.size.regular,
          lineHeight: typography.height.regular,
        }}
      >
        <Link href={`/${projectId}/visualizer?query=${encodedFilter}`} passHref>
          <Button
            aria-label="View Filter Panel"
            variant={BUTTON_VARIANTS.SECONDARY_SMALL}
            onClick={() => {
              setRightOpeningPanel({
                type: ACTIONS.OPEN,
                payload: { openPanel: 'filters' },
              })
            }}
            style={{
              display: 'flex',
              paddingTop: spacing.moderate,
              paddingBottom: spacing.moderate,
            }}
          >
            <div css={{ display: 'flex', alignItems: 'center' }}>
              <FilterSvg
                height={constants.icons.regular}
                css={{ paddingRight: spacing.base }}
              />
              View Filter Panel
            </div>
          </Button>
        </Link>

        <div css={{ height: spacing.normal }} />

        <h3>
          <span
            css={{
              fontFamily: typography.family.condensed,
              fontWeight: typography.weight.regular,
              color: colors.structure.zinc,
            }}
          >
            True:
          </span>{' '}
          {labels[selectedCell[0]]}
        </h3>
        <h3>
          <span
            css={{
              fontFamily: typography.family.condensed,
              fontWeight: typography.weight.regular,
              color: colors.structure.zinc,
            }}
          >
            Pred:
          </span>{' '}
          {labels[selectedCell[1]]}
        </h3>
      </div>

      <div
        css={{
          backgroundColor: colors.structure.coal,
          flex: 1,
        }}
      />
    </div>
  )
}

ModelMatrixPreview.propTypes = {
  selectedCell: PropTypes.arrayOf(PropTypes.number).isRequired,
  labels: PropTypes.arrayOf(PropTypes.string).isRequired,
  moduleName: PropTypes.string.isRequired,
}

export default ModelMatrixPreview
