import PropTypes from 'prop-types'
import { useRouter } from 'next/router'

import { colors, constants, spacing, typography } from '../Styles'

import ButtonCopy from '../Button/Copy'
import Button, { VARIANTS } from '../Button'

import {
  ACTIONS as FILTER_ACTIONS,
  dispatch as filterDispatch,
} from '../Filters/helpers'

import MetadataPrettyNoResults from './NoResults'

const MetadataPrettyContent = ({ path, name, value: { content } }) => {
  const {
    pathname,
    query: { projectId, assetId, query },
  } = useRouter()

  if (!content) {
    return (
      <MetadataPrettyNoResults
        name={
          <Button
            aria-label="Add Filter"
            variant={VARIANTS.NEUTRAL}
            style={{
              fontSize: 'inherit',
              lineHeight: 'inherit',
            }}
            onClick={() => {
              filterDispatch({
                type: FILTER_ACTIONS.ADD_VALUE,
                payload: {
                  pathname,
                  projectId,
                  assetId,
                  filter: {
                    type: 'textContent',
                    attribute: `${path}.${name}`,
                    values: {},
                  },
                  query,
                },
              })
            }}
          >
            {name}
          </Button>
        }
      />
    )
  }

  return (
    <>
      <div
        css={{
          '&:not(:first-of-type)': {
            borderTop: constants.borders.large.smoke,
          },
          padding: spacing.normal,
          paddingBottom: spacing.base,
          fontFamily: typography.family.mono,
          fontSize: typography.size.small,
          lineHeight: typography.height.small,
          color: colors.structure.white,
        }}
      >
        <Button
          aria-label="Add Filter"
          variant={VARIANTS.NEUTRAL}
          style={{
            fontSize: 'inherit',
            lineHeight: 'inherit',
          }}
          onClick={() => {
            filterDispatch({
              type: FILTER_ACTIONS.ADD_VALUE,
              payload: {
                pathname,
                projectId,
                assetId,
                filter: {
                  type: 'textContent',
                  attribute: `${path}.${name}`,
                  values: {},
                },
                query,
              },
            })
          }}
        >
          {name}
        </Button>
      </div>

      <div
        css={{
          padding: `${spacing.base}px ${spacing.normal}px`,
          paddingBottom: 0,
          width: '100%',
          fontFamily: typography.family.condensed,
          textTransform: 'uppercase',
          color: colors.structure.steel,
        }}
      >
        content
      </div>

      <div css={{ paddingBottom: spacing.base }}>
        <div
          css={{
            display: 'flex',
            color: colors.structure.zinc,
            svg: { opacity: 0 },
            ':hover': {
              backgroundColor: `${colors.signal.sky.base}${constants.opacity.hex22Pct}`,
              color: colors.structure.white,
              svg: { opacity: 1 },
            },
          }}
        >
          <div
            css={{
              width: '100%',
              padding: `${spacing.moderate}px ${spacing.normal}px`,
              wordBreak: 'break-word',
            }}
          >
            {content}
          </div>

          <ButtonCopy title="Content" value={content} offset={100} />
        </div>
      </div>
    </>
  )
}

MetadataPrettyContent.propTypes = {
  path: PropTypes.string.isRequired,
  name: PropTypes.string.isRequired,
  value: PropTypes.shape({
    content: PropTypes.string.isRequired,
  }).isRequired,
}

export default MetadataPrettyContent
