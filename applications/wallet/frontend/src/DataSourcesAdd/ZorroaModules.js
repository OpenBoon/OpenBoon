import PropTypes from 'prop-types'

import { colors, spacing, constants } from '../Styles'

import CheckmarkSvg from '../Icons/checkmark.svg'

import Accordion, { CHECKMARK_WIDTH } from '../Accordion'
import CheckboxTable from '../Checkbox/Table'

const MODULES = [
  {
    key: 'label-detection',
    label: 'Label Detection',
    legend: 'Adds a list of predicted label that apply to the media.',
  },
  {
    key: 'object-detection',
    label: 'Object Detection',
    legend: 'Detects up to 80 everyday objects.',
  },
  {
    key: 'facial-recognition',
    label: 'Facial Recognition',
    legend: 'Recognizes faces within an image.',
  },
  {
    key: 'ocr',
    label: 'OCR (Optical Character Recognition)',
    legend: 'Transcribes text found in images.',
  },
  {
    key: 'shot-detection',
    label: 'Shot Detection',
    legend:
      'Intelligently breaks a video into separate shots and imports each as its own entity.',
  },
  {
    key: 'page-analysis',
    label: 'Page Analysis',
    legend:
      'Breaks multipage document into individual pages and imports them as individual entities.',
  },
]

const DataSourcesAddZorroaModules = ({ onClick }) => {
  return (
    <Accordion
      title={
        <>
          <CheckmarkSvg
            width={CHECKMARK_WIDTH}
            css={{ color: colors.key.one, marginRight: spacing.normal }}
          />
          Zorroa
        </>
      }>
      <>
        <p
          css={{
            color: colors.structure.zinc,
            margin: 0,
            paddingTop: spacing.base,
            paddingBottom: spacing.normal,
            maxWidth: constants.paragraph.maxWidth,
          }}>
          These analysis modules are included in your base package. You can run
          as many as you’d like,  but running more than you need will increase
          processing time.
        </p>
        <CheckboxTable options={MODULES} onClick={onClick} />
      </>
    </Accordion>
  )
}

DataSourcesAddZorroaModules.propTypes = {
  onClick: PropTypes.func.isRequired,
}

export default DataSourcesAddZorroaModules
