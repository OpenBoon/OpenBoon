import PropTypes from 'prop-types'
import useResizeObserver from 'use-resize-observer'
import { Responsive as ResponsiveGridLayout } from 'react-grid-layout'

import chartShape from '../Chart/shape'

import { spacing } from '../Styles'

import ChartFacet from '../ChartFacet'
import ChartRange from '../ChartRange'
import ChartHistogram from '../ChartHistogram'

import { MIN_ROW_HEIGHT, breakpoints, cols, setAllLayouts } from './helpers'

const Charts = ({ charts, layouts, dispatch, setLayouts }) => {
  const { ref, width = 1200 } = useResizeObserver()

  return (
    <div ref={ref}>
      <ResponsiveGridLayout
        width={width}
        layouts={layouts}
        breakpoints={breakpoints}
        cols={cols}
        margin={[spacing.normal, spacing.normal]}
        containerPadding={[0, 0]}
        rowHeight={MIN_ROW_HEIGHT}
        onLayoutChange={setAllLayouts({ charts, setLayouts })}
      >
        {charts
          .filter(({ id }) => !!id)
          .map((chart, index) => {
            switch (chart.type) {
              case 'facet': {
                return (
                  <div key={chart.id}>
                    <ChartFacet
                      chart={chart}
                      chartIndex={index}
                      dispatch={dispatch}
                    />
                  </div>
                )
              }

              case 'range': {
                return (
                  <div key={chart.id}>
                    <ChartRange
                      chart={chart}
                      chartIndex={index}
                      dispatch={dispatch}
                    />
                  </div>
                )
              }

              case 'histogram': {
                return (
                  <div key={chart.id}>
                    <ChartHistogram
                      chart={chart}
                      chartIndex={index}
                      dispatch={dispatch}
                    />
                  </div>
                )
              }

              default:
                return null
            }
          })}
      </ResponsiveGridLayout>
    </div>
  )
}

Charts.propTypes = {
  charts: PropTypes.arrayOf(PropTypes.shape(chartShape)).isRequired,
  layouts: PropTypes.shape({}).isRequired,
  dispatch: PropTypes.func.isRequired,
  setLayouts: PropTypes.func.isRequired,
}

export default Charts
