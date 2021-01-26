import { useRouter } from 'next/router'
import Link from 'next/link'
import PropTypes from 'prop-types'

import { colors, constants, spacing, typography } from '../Styles'

import { useLocalStorage } from '../LocalStorage/helpers'

import FilterSvg from '../Icons/filter.svg'

import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'
import { MIN_WIDTH as PANEL_MIN_WIDTH } from '../Panel'
import { ACTIONS, reducer as resizeableReducer } from '../Resizeable/reducer'

import SuspenseBoundary from '../SuspenseBoundary'

import { encode } from '../Filters/helpers'

import ModelMatrixPreviewContent from './Content'

const ModelMatrixPreview = ({
  settings: { selectedCell, minScore, maxScore },
  labels,
  moduleName,
}) => {
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
        values: {
          labels: [labels[selectedCell[0]]],
          scope: 'test',
        },
      },
      {
        attribute: `analysis.${moduleName}`,
        isDisabled: false,
        type: 'labelConfidence',
        values: {
          labels: [labels[selectedCell[1]]],
          min: minScore,
          max: maxScore,
        },
      },
    ],
  })

  return (
    <div
      css={{
        flex: 1,
        display: 'flex',
        flexDirection: 'column',
        borderLeft: constants.borders.regular.coal,
        overflow: 'hidden',
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

      <SuspenseBoundary>
        <ModelMatrixPreviewContent
          encodedFilter={encodedFilter}
          projectId={projectId}
        />
      </SuspenseBoundary>
    </div>
  )
}

ModelMatrixPreview.propTypes = {
  settings: PropTypes.shape({
    selectedCell: PropTypes.arrayOf(PropTypes.number.isRequired).isRequired,
    minScore: PropTypes.number.isRequired,
    maxScore: PropTypes.number.isRequired,
  }).isRequired,
  labels: PropTypes.arrayOf(PropTypes.string).isRequired,
  moduleName: PropTypes.string.isRequired,
}

export default ModelMatrixPreview
