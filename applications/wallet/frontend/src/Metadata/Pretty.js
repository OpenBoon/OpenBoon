import Accordion, { VARIANTS } from '../Accordion'

const MetadataPretty = () => {
  return (
    <div css={{ height: '100%', overflow: 'auto' }}>
      <Accordion
        variant={VARIANTS.PANEL}
        title="System"
        isInitiallyOpen={false}
      >
        <div />
      </Accordion>
      <Accordion
        variant={VARIANTS.PANEL}
        title="Source"
        isInitiallyOpen={false}
      >
        <div />
      </Accordion>
      <Accordion variant={VARIANTS.PANEL} title="Files" isInitiallyOpen={false}>
        <div />
      </Accordion>
      <Accordion
        variant={VARIANTS.PANEL}
        title="Metrics"
        isInitiallyOpen={false}
      >
        <div />
      </Accordion>
      <Accordion variant={VARIANTS.PANEL} title="Media" isInitiallyOpen={false}>
        <div />
      </Accordion>
      <Accordion
        variant={VARIANTS.PANEL}
        title="Analysis"
        isInitiallyOpen={false}
      >
        <div />
      </Accordion>
      <Accordion variant={VARIANTS.PANEL} title="Clip" isInitiallyOpen={false}>
        <div />
      </Accordion>
    </div>
  )
}

export default MetadataPretty
