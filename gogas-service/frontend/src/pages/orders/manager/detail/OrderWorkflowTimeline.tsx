import React, { useMemo, useState, useEffect } from 'react';
import { Paper, Stepper, Step, StepLabel, Typography, Chip, Tooltip, useMediaQuery, Collapse, Fade, IconButton, ClickAwayListener } from '@material-ui/core';
import {
  LockOpenSharp as OpenedIcon,
  FolderSharp as ClosedIcon,
  SendSharp as SentIcon,
  SyncSharp as SyncedIcon,
  FitnessCenterSharp as WeightsIcon,
  AttachMoneySharp as AccountedIcon,
  CheckSharp as CheckIcon,
  BlockSharp as CancelledIcon,
  InfoOutlined as InfoIcon,
  ExpandMoreSharp as ExpandMoreIcon,
  ExpandLessSharp as ExpandLessIcon,
} from '@material-ui/icons';
import { makeStyles, useTheme } from '@material-ui/core/styles';
import { StepIconProps } from '@material-ui/core/StepIcon';
import moment from 'moment-timezone';
import { OrderDetails } from './types';

const useStyles = makeStyles((theme) => ({
  panel: { padding: theme.spacing(1, 2), marginBottom: theme.spacing(2) },
  cancelled: { display: 'flex', alignItems: 'center', gap: theme.spacing(1) },
  collapsedHeader: {
    display: 'flex', alignItems: 'center', gap: theme.spacing(1),
  },
  collapsedCurrent: {
    display: 'inline-flex', alignItems: 'center', gap: theme.spacing(1),
    flex: '1 1 auto', minWidth: 0,
  },
  currentDot: {
    display: 'flex', alignItems: 'center', justifyContent: 'center',
    flex: '0 0 auto',
  },
  iconRoot: {
    display: 'flex', alignItems: 'center', justifyContent: 'center',
    width: 32, height: 32, borderRadius: '50%',
    color: theme.palette.grey[500],
    backgroundColor: theme.palette.grey[300],
  },
  iconActive: {
    color: theme.palette.primary.contrastText,
    backgroundColor: theme.palette.primary.main,
  },
  iconCompleted: {
    color: theme.palette.primary.contrastText,
    backgroundColor: theme.palette.success.main,
  },
  iconPending: {
    color: theme.palette.getContrastText(theme.palette.warning.main),
    backgroundColor: theme.palette.warning.main,
  },
  label: {
    display: 'inline-flex',
    alignItems: 'center',
    gap: theme.spacing(0.5),
  },
  infoIcon: {
    fontSize: '1rem',
    cursor: 'help',
  },
  infoIconPending: {
    color: theme.palette.warning.main,
  },
  infoIconDone: {
    color: theme.palette.text.secondary,
  },
}));

interface WorkflowStep {
  key: string;
  label: string;
  icon: React.ReactElement;
  /** Whether this step has been reached/completed, from the order details. */
  completed: boolean;
  /**
   * The step represents a required-but-not-done action that is now pending
   * (e.g. order closed but not yet sent to Aequos). Shown with a warning
   * colour instead of the normal "active" highlight.
   */
  pending?: boolean;
  /** Optional reminder shown as an info icon + tooltip next to the label. */
  info?: string;
}

/**
 * Horizontal timeline of the order-management workflow, with labels stacked
 * under each step (MUI Stepper `alternativeLabel`). The step set depends on
 * whether the order is linked to Aequos:
 *  - standard:  Aperto -> Chiuso -> Contabilizzato
 *  - Aequos:    Aperto -> Chiuso -> Inviato -> Sincronizzato -> [Invio pesi] -> Contabilizzato
 * Cancelled orders show a dedicated "Annullato" indicator instead.
 */
/**
 * Info icon with a tooltip that works on both desktop and touch. MUI tooltips
 * only open on hover, which never fires on touch devices, so we also open it
 * on tap (click) and dismiss it with a click-away.
 */
const InfoTooltip: React.FC<{ title: string; className: string }> = ({ title, className }) => {
  const [open, setOpen] = useState(false);
  return (
    <ClickAwayListener onClickAway={() => setOpen(false)}>
      <Tooltip
        title={title}
        open={open}
        onClose={() => setOpen(false)}
        onOpen={() => setOpen(true)}
        disableTouchListener
      >
        <InfoIcon
          fontSize="inherit"
          className={className}
          onClick={(e) => {
            e.stopPropagation();
            setOpen((o) => !o);
          }}
        />
      </Tooltip>
    </ClickAwayListener>
  );
};

const OrderWorkflowTimeline: React.FC<{ details?: OrderDetails }> = ({ details }) => {
  const classes = useStyles();
  const theme = useTheme();
  // On phones a horizontal stepper with up to 6 labelled steps cannot fit.
  // Switch to a vertical stepper (labels beside the icons), the standard
  // mobile pattern, which fits any width.
  const isMobile = useMediaQuery(theme.breakpoints.down('sm'));
  const vertical = isMobile;
  // On mobile the timeline is collapsible and starts collapsed, showing only
  // the current order status; on desktop it is always expanded.
  const [expanded, setExpanded] = useState(!isMobile);
  // Keep the expanded/collapsed default in sync when crossing the breakpoint.
  useEffect(() => setExpanded(!isMobile), [isMobile]);

  const { steps, activeStep, cancelled } = useMemo(() => {
    if (!details) {
      return { steps: [] as WorkflowStep[], activeStep: 0, cancelled: false };
    }

    // Order is Cancelled when neither closed/editable nor accounted AND it was
    // explicitly annullato. We don't have a direct flag, so treat the dedicated
    // 'undocancel'-only action set as cancelled.
    const isCancelled = (details.actions ?? '').split(',').map((a) => a.trim()).includes('undocancel');

    const isAequos = details.idaequos != null;
    const closed = details.editable || details.contabilizzato || details.inviato || !!details.sincronizzato;
    const accounted = details.contabilizzato;
    const sent = details.inviato && details.numeroOrdineEsterno != null;
    const synced = details.sincronizzato != null;
    const weightsSent = details.pesiInviati != null;

    // Still open (not closed and not accounted) -> closing is the pending action.
    const closePending = !closed && !accounted;
    const list: WorkflowStep[] = [
      { key: 'opened', label: 'Aperto', icon: <OpenedIcon fontSize="small" />, completed: true },
      {
        key: 'closed',
        label: closePending ? 'Da chiudere' : 'Chiuso',
        icon: <ClosedIcon fontSize="small" />,
        completed: closed,
        pending: closePending,
        info: closePending
          ? `L'ordine può essere chiuso dopo la data di chiusura (${details.datachiusura ?? 'data di chiusura'}).`
          : undefined,
      },
    ];

    if (isAequos) {
      // Closed but not yet sent -> pending "Da inviare ad Aequos".
      const sendPending = closed && !sent && !accounted;
      list.push({
        key: 'sent',
        label: sendPending ? 'Da inviare ad Aequos' : 'Inviato ad Aequos',
        icon: <SentIcon fontSize="small" />,
        completed: sent,
        pending: sendPending,
        info: sent
          ? `Ordine inviato ad Aequos (numero ordine: ${details.numeroOrdineEsterno}).`
          : sendPending
            ? `L'ordine deve essere inviato ad Aequos entro la mezzanotte del ${details.datachiusura ?? 'giorno di chiusura'}.`
            : undefined,
      });
      // Sent but not yet synced -> pending "Da sincronizzare".
      const syncPending = sent && !synced && !accounted;
      list.push({
        key: 'synced',
        label: syncPending ? 'Da sincronizzare' : 'Sincronizzato',
        icon: <SyncedIcon fontSize="small" />,
        completed: synced,
        pending: syncPending,
        info: synced
          ? `Ultima sincronizzazione: ${details.sincronizzato}.`
          : syncPending
            ? 'Sincronizza per ottenere le variazioni da Aequos.'
            : undefined,
      });
      if (details.invioPesiRichiesto) {
        const weightsPending = synced && !weightsSent && !accounted;
        // Weights must be sent within 4 days of the delivery date.
        const weightsDeadline = details.dataconsegna
          ? moment(details.dataconsegna, 'DD/MM/YYYY').add(4, 'days').format('DD/MM/YYYY')
          : undefined;
        list.push({
          key: 'weights',
          label: weightsPending ? 'Pesi da inviare' : 'Pesi inviati',
          icon: <WeightsIcon fontSize="small" />,
          completed: weightsSent,
          pending: weightsPending,
          info: weightsSent
            ? `Pesi inviati ad Aequos il ${details.pesiInviati}.`
            : weightsPending
              ? `I pesi devono essere inviati ad Aequos entro il ${weightsDeadline ?? 'termine previsto'}.`
              : undefined,
        });
      }
    }

    // The accounting step becomes pending once its predecessor is done:
    //  - Aequos Fresco (weights required) -> after weights sent
    //  - Aequos (no weights)              -> after synced
    //  - standard                         -> after closed
    const readyToAccount = isAequos
      ? details.invioPesiRichiesto
        ? weightsSent
        : synced
      : closed;
    const accountPending = readyToAccount && !accounted;
    list.push({
      key: 'accounted',
      label: accountPending ? 'Da contabilizzare' : 'Contabilizzato',
      icon: <AccountedIcon fontSize="small" />,
      completed: accounted,
      pending: accountPending,
      info: accountPending
        ? 'Contabilizza per addebitare gli importi ai gasisti.'
        : undefined,
    });

    // Active step = first non-completed, or the last one when all complete.
    const firstIncomplete = list.findIndex((s) => !s.completed);
    const active = firstIncomplete === -1 ? list.length - 1 : firstIncomplete;

    return { steps: list, activeStep: active, cancelled: isCancelled };
  }, [details]);

  if (!details) return null;

  if (cancelled) {
    return (
      <Paper className={classes.panel} variant="outlined">
        <div className={classes.cancelled}>
          <CancelledIcon color="error" fontSize="small" />
          <Typography variant="body2">
            Ordine <b>annullato</b>. Ripristinare l'ordine per riprendere la gestione.
          </Typography>
          <Chip size="small" label="Annullato" color="secondary" />
        </div>
      </Paper>
    );
  }

  const renderIcon = (step: WorkflowStep) => (props: StepIconProps) => {
    const { active, completed } = props;
    const stateCls = completed
      ? classes.iconCompleted
      : step.pending
        ? classes.iconPending
        : active
          ? classes.iconActive
          : '';
    return (
      <div className={`${classes.iconRoot} ${stateCls}`}>
        {completed ? <CheckIcon fontSize="small" /> : step.icon}
      </div>
    );
  };

  // Core order status shown in the collapsed summary — just the three primary
  // states, regardless of the finer Aequos sub-steps:
  //   accounted -> Contabilizzato, editable (closed) -> Chiuso, otherwise Aperto.
  const coreStatus = details.contabilizzato
    ? { label: 'Contabilizzato', icon: <AccountedIcon fontSize="small" />, color: theme.palette.success.main }
    : details.editable
      ? { label: 'Chiuso', icon: <ClosedIcon fontSize="small" />, color: theme.palette.primary.main }
      : { label: 'Aperto', icon: <OpenedIcon fontSize="small" />, color: theme.palette.primary.main };

  return (
    <Paper className={classes.panel} variant="outlined">
      {isMobile ? (
        <div className={classes.collapsedHeader}>
          <span className={classes.collapsedCurrent}>
            {expanded ? (
              <Typography variant="body2" noWrap>
                <b>Stato ordine</b>
              </Typography>
            ) : (
              <>
                <span className={classes.currentDot} style={{ color: coreStatus.color }}>
                  {coreStatus.icon}
                </span>
                <Typography variant="body2" noWrap>
                  <b>Stato:</b> {coreStatus.label}
                </Typography>
              </>
            )}
          </span>
          <IconButton
            size="small"
            aria-label={expanded ? 'Nascondi timeline' : 'Mostra timeline'}
            onClick={() => setExpanded((e) => !e)}
          >
            {expanded ? <ExpandLessIcon /> : <ExpandMoreIcon />}
          </IconButton>
        </div>
      ) : null}

      <Collapse in={expanded} timeout={300} unmountOnExit>
      <Fade in={expanded} timeout={{ enter: 400, exit: 150 }}>
      <Stepper
        alternativeLabel={!vertical}
        orientation={vertical ? 'vertical' : 'horizontal'}
        activeStep={activeStep}
        style={{ padding: 8, background: 'transparent' }}
      >
        {steps.map((step) => (
          <Step key={step.key} completed={step.completed}>
            <StepLabel StepIconComponent={renderIcon(step)}>
              <span className={classes.label}>
                {step.label}
                {step.info ? (
                  <InfoTooltip
                    title={step.info}
                    className={`${classes.infoIcon} ${
                      step.completed ? classes.infoIconDone : classes.infoIconPending
                    }`}
                  />
                ) : null}
              </span>
            </StepLabel>
          </Step>
        ))}
      </Stepper>
      </Fade>
      </Collapse>
    </Paper>
  );
};

export default OrderWorkflowTimeline;
